package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.AuthorType
import com.budgetntr.proto.recipe.v1.ContentVisibility
import com.budgetntr.proto.recipe.v1.CreateRecipeRequest
import com.budgetntr.proto.recipe.v1.ForkRecipeRequest
import com.budgetntr.proto.recipe.v1.GetRecipeRequest
import com.budgetntr.proto.recipe.v1.PublishRecipeRequest
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import com.budgetntr.proto.recipe.v1.RecipeIngredientProto
import com.budgetntr.proto.recipe.v1.SearchRecipesRequest
import com.budgetntr.proto.recipe.v1.UpdateRecipeRequest
import dev.andre.nutritionist.recipe.infrastructure.grpc.RecipeGrpcService
import dev.andre.nutritionist.recipe.infrastructure.grpc.RecipeIdentityInterceptor
import dev.andre.nutritionist.recipe.infrastructure.grpc.client.CatalogClient
import dev.andre.nutritionist.recipe.persistence.repository.RecipeRepository
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.Server
import io.grpc.ServerInterceptors
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class RecipeCatalogGrpcIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var recipeGrpcService: RecipeGrpcService

    @Autowired
    private lateinit var recipeIdentityInterceptor: RecipeIdentityInterceptor

    @Autowired
    private lateinit var recipeRepository: RecipeRepository

    private lateinit var server: Server
    private lateinit var channel: ManagedChannel
    private lateinit var stub: RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineStub

    @BeforeEach
    fun setUp() {
        val serverName = InProcessServerBuilder.generateName()
        server =
            InProcessServerBuilder
                .forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(recipeGrpcService, recipeIdentityInterceptor))
                .build()
                .start()

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        stub = RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineStub(channel)
    }

    @AfterEach
    fun tearDown() {
        channel.shutdownNow()
        server.shutdownNow()
    }

    private fun stubForUser(
        userId: String,
        roles: String = "ROLE_USER",
    ): RecipeAiServiceGrpcKt.RecipeAiServiceCoroutineStub {
        val headers =
            Metadata().apply {
                put(CatalogClient.userHeader, userId)
                put(CatalogClient.devBypassHeader, "true")
                put(Metadata.Key.of("x-user-roles", Metadata.ASCII_STRING_MARSHALLER), roles)
            }
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
    }

    @Test
    fun `search system recipes returns seeded recipes with pagination`() =
        runTest {
            val client = stubForUser("usr_search_test")
            val request =
                SearchRecipesRequest
                    .newBuilder()
                    .setAuthorType(AuthorType.AUTHOR_TYPE_SYSTEM)
                    .setVisibility(ContentVisibility.CONTENT_VISIBILITY_PUBLIC)
                    .setPageSize(10)
                    .build()

            val response = client.searchRecipes(request)
            assertTrue(response.recipesCount >= 10, "Should return at least 10 system recipes")
            assertTrue(response.recipesList.all { it.authorType == AuthorType.AUTHOR_TYPE_SYSTEM })
            assertTrue(response.nextPageToken.isNotBlank(), "Next page token should not be blank")

            // Next page
            val nextRequest =
                SearchRecipesRequest
                    .newBuilder()
                    .setAuthorType(AuthorType.AUTHOR_TYPE_SYSTEM)
                    .setPageSize(10)
                    .setPageToken(response.nextPageToken)
                    .build()
            val nextResponse = client.searchRecipes(nextRequest)
            assertTrue(nextResponse.recipesCount > 0, "Second page should contain items")
        }

    @Test
    fun `create user recipe calculates nutrition per 100g correctly`() =
        runTest {
            val userId = "usr_create_test"
            val client = stubForUser(userId)

            // 100g product A (100 kcal, 10g prot, 5g fat, 20g carb) + 100g product B (200 kcal, 20g prot, 10g fat, 40g carb)
            // Total weight = 200g. Per 100g = 150 kcal, 15g prot, 7.5g fat, 30g carb
            val ing1 =
                RecipeIngredientProto
                    .newBuilder()
                    .setProductNameSnapshot("Ингредиент 1")
                    .setWeightGrams(100.0)
                    .setCaloriesPer100G(100.0)
                    .setProteinsPer100G(10.0)
                    .setFatsPer100G(5.0)
                    .setCarbsPer100G(20.0)
                    .build()
            val ing2 =
                RecipeIngredientProto
                    .newBuilder()
                    .setProductNameSnapshot("Ингредиент 2")
                    .setWeightGrams(100.0)
                    .setCaloriesPer100G(200.0)
                    .setProteinsPer100G(20.0)
                    .setFatsPer100G(10.0)
                    .setCarbsPer100G(40.0)
                    .build()

            val createReq =
                CreateRecipeRequest
                    .newBuilder()
                    .setTitle("Тестовый омлет")
                    .setDescription("Описание омлета")
                    .setInstructions("Взбейте и пожарьте")
                    .setVisibility(ContentVisibility.CONTENT_VISIBILITY_PRIVATE)
                    .setServingsCount(2)
                    .addAllIngredients(listOf(ing1, ing2))
                    .build()

            val response = client.createRecipe(createReq)
            val created = response.recipe

            assertNotNull(created.id)
            assertEquals(userId, created.authorId)
            assertEquals(200.0, created.totalWeightG, 0.01)
            assertEquals(150.0, created.caloriesPer100G, 0.01)
            assertEquals(15.0, created.proteinsPer100G, 0.01)
            assertEquals(7.5, created.fatsPer100G, 0.01)
            assertEquals(30.0, created.carbsPer100G, 0.01)
            assertEquals(2, created.ingredientsCount)
        }

    @Test
    fun `fork system recipe copies data and isolates from origin`() =
        runTest {
            val user1 = "usr_fork_1"
            val client = stubForUser(user1)

            // Find a system recipe
            val search =
                client.searchRecipes(
                    SearchRecipesRequest
                        .newBuilder()
                        .setAuthorType(AuthorType.AUTHOR_TYPE_SYSTEM)
                        .setPageSize(1)
                        .build(),
                )
            val origin = search.recipesList.first()

            val forkReq =
                ForkRecipeRequest
                    .newBuilder()
                    .setSourceRecipeId(origin.id)
                    .setNewTitle("Моя кастомная овсянка")
                    .build()

            val forkResp = client.forkRecipe(forkReq)
            val forked = forkResp.recipe

            assertNotNull(forked.id)
            assertTrue(forked.id != origin.id)
            assertEquals(origin.id, forked.originRecipeId)
            assertEquals(user1, forked.authorId)
            assertEquals("Моя кастомная овсянка", forked.title)
            assertEquals(ContentVisibility.CONTENT_VISIBILITY_PRIVATE, forked.visibility)

            // Update forked recipe
            val updateReq =
                UpdateRecipeRequest
                    .newBuilder()
                    .setRecipeId(forked.id)
                    .setTitle("Овсянка с бананом")
                    .setVisibility(ContentVisibility.CONTENT_VISIBILITY_PRIVATE)
                    .setServingsCount(1)
                    .addAllIngredients(forked.ingredientsList)
                    .build()
            val updateResp = client.updateRecipe(updateReq)
            assertEquals("Овсянка с бананом", updateResp.recipe.title)

            // Verify original is untouched
            val getOrigin = client.getRecipe(GetRecipeRequest.newBuilder().setRecipeId(origin.id).build())
            assertEquals(origin.title, getOrigin.recipe.title)
        }

    @Test
    fun `publish recipe changes visibility and other users can view it`() =
        runTest {
            val owner = "usr_owner_pub"
            val otherUser = "usr_other_reader"
            val ownerClient = stubForUser(owner)
            val otherClient = stubForUser(otherUser)

            val createReq =
                CreateRecipeRequest
                    .newBuilder()
                    .setTitle("Секретный десерт")
                    .setVisibility(ContentVisibility.CONTENT_VISIBILITY_PRIVATE)
                    .setServingsCount(1)
                    .build()
            val recipe = ownerClient.createRecipe(createReq).recipe

            // Other user cannot view private recipe
            val err =
                assertThrows<StatusException> {
                    otherClient.getRecipe(GetRecipeRequest.newBuilder().setRecipeId(recipe.id).build())
                }
            assertEquals(Status.Code.PERMISSION_DENIED, err.status.code)

            // Owner publishes recipe
            val publishReq =
                PublishRecipeRequest
                    .newBuilder()
                    .setRecipeId(recipe.id)
                    .setVisibility(ContentVisibility.CONTENT_VISIBILITY_PUBLIC)
                    .build()
            val published = ownerClient.publishRecipe(publishReq).recipe
            assertEquals(ContentVisibility.CONTENT_VISIBILITY_PUBLIC, published.visibility)

            // Now other user can view it
            val readByOther = otherClient.getRecipe(GetRecipeRequest.newBuilder().setRecipeId(recipe.id).build())
            assertEquals(recipe.id, readByOther.recipe.id)
            assertEquals("Секретный десерт", readByOther.recipe.title)
        }
}
