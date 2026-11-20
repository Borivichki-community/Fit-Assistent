package dev.andre.nutritionist.recipe

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.grpc.server.port=0", "management.server.port=0"],
)
class RecipeApplicationTest {
    @Test
    fun startsWithDefaultConfiguration() = Unit
}
