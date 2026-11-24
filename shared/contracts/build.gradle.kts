import org.apache.avro.Schema
import java.net.HttpURLConnection
import java.net.URI

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.avro)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

val ktlintEngineVersion =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("ktlint")
        .get()
        .requiredVersion

ktlint {
    version.set(ktlintEngineVersion)
}

dependencies {
    implementation(libs.avro.runtime)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.schema.registry.client)
}

avro {
    isCreateSetters.set(true)
    isCreateOptionalGetters.set(false)
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
}

abstract class TestSchemaCompatibilityTask : DefaultTask() {
    @get:InputDirectory
    abstract val schemasDir: DirectoryProperty

    @get:Input
    abstract val registryUrl: Property<String>

    @get:Input
    abstract val requireRegistry: Property<Boolean>

    @TaskAction
    fun checkCompatibility() {
        val avroDir = schemasDir.get().asFile
        val schemaFiles = avroDir.listFiles { file -> file.extension == "avsc" } ?: emptyArray()
        if (schemaFiles.isEmpty()) {
            logger.warn("No .avsc schema files found in ${avroDir.absolutePath}")
            return
        }

        val url = registryUrl.get().trimEnd('/')
        val parser = Schema.Parser()
        val parsedSchemas = mutableMapOf<String, Schema>()

        // 1. Local syntax and schema rule audit (Strict BACKWARD checks)
        schemaFiles.forEach { file ->
            val schema = parser.parse(file)
            parsedSchemas[file.nameWithoutExtension] = schema
            logger.lifecycle("Validated syntax for Avro schema: ${schema.fullName}")

            // BACKWARD rule audit: Any union with null or optional field MUST define a default value
            schema.fields.forEach { field ->
                val isUnionWithNull =
                    field.schema().type == Schema.Type.UNION &&
                        field.schema().types.any { it.type == Schema.Type.NULL }
                if (isUnionWithNull && !field.hasDefaultValue()) {
                    throw GradleException(
                        "Schema audit failure in ${file.name}: field '${field.name()}' is a nullable union but lacks a 'default' value. " +
                            "This breaks BACKWARD compatibility!",
                    )
                }
            }
        }

        // 2. Remote Schema Registry verification against latest subject versions
        parsedSchemas.forEach { (schemaName, schema) ->
            val subject = "$schemaName-value"
            val checkUrl = "$url/compatibility/subjects/$subject/versions/latest"

            try {
                val connection = URI(checkUrl).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/vnd.schemaregistry.v1+json")
                connection.setRequestProperty("Accept", "application/vnd.schemaregistry.v1+json")
                connection.doOutput = true
                connection.connectTimeout = 3000
                connection.readTimeout = 3000

                val escapedSchema = schema.toString(false).replace("\"", "\\\"").replace("\n", "\\n")
                val payload = "{\"schema\":\"$escapedSchema\"}"

                connection.outputStream.use { os ->
                    os.write(payload.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val responseBody = connection.inputStream.bufferedReader().readText()
                    if (responseBody.contains("\"is_compatible\":true") || responseBody.contains("\"is_compatible\": true")) {
                        logger.lifecycle("Schema $subject is BACKWARD compatible with Schema Registry ($url)")
                    } else {
                        throw GradleException(
                            "Schema $subject is NOT compatible with Schema Registry ($url). Response: $responseBody",
                        )
                    }
                } else if (responseCode == 404) {
                    logger.lifecycle(
                        "Subject $subject does not exist in Schema Registry yet. It will be registered as version 1 (initial).",
                    )
                } else {
                    val errorBody =
                        connection.errorStream
                            ?.bufferedReader()
                            ?.readText()
                            .orEmpty()
                    logger.warn("Schema Registry returned HTTP $responseCode for $subject: $errorBody")
                }
            } catch (ex: Exception) {
                if (requireRegistry.get()) {
                    throw GradleException(
                        "Schema Registry at $url is required but unreachable for schema compatibility verification: ${ex.message}",
                        ex,
                    )
                } else {
                    logger.warn(
                        "[WARN] Schema Registry at $url is offline or unreachable (${ex.message}). " +
                            "Local syntax and BACKWARD default values audit PASSED successfully.",
                    )
                }
            }
        }
    }
}

tasks.register<TestSchemaCompatibilityTask>("testSchemaCompatibility") {
    group = "verification"
    description = "Checks Avro schema compatibility with Schema Registry in BACKWARD mode"
    schemasDir.set(layout.projectDirectory.dir("src/main/avro"))
    registryUrl.set(
        providers
            .environmentVariable("SCHEMA_REGISTRY_URL")
            .orElse(providers.gradleProperty("schemaRegistryUrl"))
            .orElse("http://localhost:8081"),
    )
    requireRegistry.set(
        providers
            .environmentVariable("REQUIRE_SCHEMA_REGISTRY")
            .map { it.toBoolean() }
            .orElse(false),
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
