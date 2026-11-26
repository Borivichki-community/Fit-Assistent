package dev.andre.nutritionist.recipe

import com.budgetntr.proto.recipe.v1.AccessMode
import com.budgetntr.proto.recipe.v1.CoachLinkStatus
import com.budgetntr.proto.recipe.v1.ContentVisibility
import com.budgetntr.proto.recipe.v1.CreateMealPlanRequest
import com.budgetntr.proto.recipe.v1.GetMealPlanRequest
import com.budgetntr.proto.recipe.v1.GrantAccessRequest
import com.budgetntr.proto.recipe.v1.GrantSource
import com.budgetntr.proto.recipe.v1.InviteStudentRequest
import com.budgetntr.proto.recipe.v1.MealPlanItemProto
import com.budgetntr.proto.recipe.v1.MealType
import com.budgetntr.proto.recipe.v1.PlanItemType
import com.budgetntr.proto.recipe.v1.RecipeAiServiceGrpcKt
import com.budgetntr.proto.recipe.v1.RespondCoachInviteRequest
import dev.andre.nutritionist.recipe.infrastructure.grpc.RecipeGrpcService
import dev.andre.nutritionist.recipe.infrastructure.grpc.RecipeIdentityInterceptor
import dev.andre.nutritionist.recipe.infrastructure.grpc.client.CatalogClient
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

class CoachingAccessIntegrationTest : AbstractPostgresIntegrationTest() {
    @Autowired
    private lateinit var recipeGrpcService: RecipeGrpcService

    @Autowired
    private lateinit var recipeIdentityInterceptor: RecipeIdentityInterceptor

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
    fun `coach student invite and response flow`() =
        runTest {
            val coachId = "usr_coach_1"
            val studentId = "usr_student_1"
            val coachClient = stubForUser(coachId, "ROLE_COACH")
            val studentClient = stubForUser(studentId, "ROLE_USER")

            // 1. Coach invites student
            val inviteReq =
                InviteStudentRequest
                    .newBuilder()
                    .setStudentId(studentId)
                    .setCanViewDiary(true)
                    .build()
            val inviteResp = coachClient.inviteStudent(inviteReq)
            val link = inviteResp.link
            assertNotNull(link.id)
            assertEquals(coachId, link.coachId)
            assertEquals(studentId, link.studentId)
            assertEquals(CoachLinkStatus.COACH_LINK_STATUS_PENDING, link.status)

            // 2. Student accepts invite
            val respondReq =
                RespondCoachInviteRequest
                    .newBuilder()
                    .setLinkId(link.id)
                    .setAccept(true)
                    .build()
            val respondResp = studentClient.respondCoachInvite(respondReq)
            assertEquals(CoachLinkStatus.COACH_LINK_STATUS_ACTIVE, respondResp.link.status)

            // 3. Verify access
            val checkReq =
                com.budgetntr.proto.recipe.v1.CheckCoachAccessRequest
                    .newBuilder()
                    .setCoachId(coachId)
                    .setStudentId(studentId)
                    .build()
            val checkResp = coachClient.checkCoachAccess(checkReq)
            assertTrue(checkResp.hasAccess)
            assertTrue(checkResp.canViewDiary)

            val listReq =
                com.budgetntr.proto.recipe.v1.ListCoachLinksRequest
                    .getDefaultInstance()
            assertEquals(1, studentClient.listCoachLinks(listReq).linksCount)
            assertEquals(1, coachClient.listCoachLinks(listReq).linksCount)
        }

    @Test
    fun `paid meal plan access denied to student until coach grants access`() =
        runTest {
            val coachId = "usr_coach_2"
            val studentId = "usr_student_2"
            val coachClient = stubForUser(coachId, "ROLE_COACH")
            val studentClient = stubForUser(studentId, "ROLE_USER")

            // 1. Coach creates paid meal plan
            val item1 =
                MealPlanItemProto
                    .newBuilder()
                    .setDayNumber(1)
                    .setMealSlot(MealType.MEAL_TYPE_BREAKFAST)
                    .setItemType(PlanItemType.PLAN_ITEM_TYPE_PRODUCT)
                    .setItemId(UUID.randomUUID().toString())
                    .setNameSnapshot("Протеиновый батончик")
                    .setWeightGrams(60.0)
                    .setCalories(210.0)
                    .setProteins(20.0)
                    .setFats(7.0)
                    .setCarbs(18.0)
                    .build()

            val createPlanReq =
                CreateMealPlanRequest
                    .newBuilder()
                    .setTitle("План сушки 2026")
                    .setDescription("Интенсивный план для атлетов")
                    .setDurationDays(7)
                    .setVisibility(ContentVisibility.CONTENT_VISIBILITY_PUBLIC)
                    .setAccessMode(AccessMode.ACCESS_MODE_PAID)
                    .setPriceAmountMinor(149900L) // 1499.00 RUB
                    .setCurrency("RUB")
                    .addItems(item1)
                    .build()

            val plan = coachClient.createMealPlan(createPlanReq).mealPlan
            assertNotNull(plan.id)
            assertEquals(1, plan.itemsCount)

            // 2. Student attempts to read paid plan -> sees plan metadata, but items list is hidden (0 items)
            val readReq = GetMealPlanRequest.newBuilder().setPlanId(plan.id).build()
            val studentPreview = studentClient.getMealPlan(readReq).mealPlan
            assertEquals("План сушки 2026", studentPreview.title)
            assertEquals(0, studentPreview.itemsCount, "Items should be hidden from student without grant")

            // 3. Coach grants access to student
            val grantReq =
                GrantAccessRequest
                    .newBuilder()
                    .setUserId(studentId)
                    .setTargetType("MEAL_PLAN")
                    .setTargetId(plan.id)
                    .setGrantSource(GrantSource.GRANT_SOURCE_COACH_GRANT)
                    .build()
            val grantResp = coachClient.grantAccess(grantReq)
            assertNotNull(grantResp.grant.id)

            // 4. Student reads plan again -> items are now fully available
            val studentFull = studentClient.getMealPlan(readReq).mealPlan
            assertEquals(1, studentFull.itemsCount, "Items should be available after grant")
            assertEquals("Протеиновый батончик", studentFull.itemsList.first().nameSnapshot)
        }
}
