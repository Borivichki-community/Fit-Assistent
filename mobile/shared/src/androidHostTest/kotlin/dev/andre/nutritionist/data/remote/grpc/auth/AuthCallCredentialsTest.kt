package dev.andre.nutritionist.data.remote.grpc.auth

import dev.andre.nutritionist.domain.auth.AuthTokenProvider
import dev.andre.nutritionist.fake.TestCoroutineDispatchers
import io.grpc.Attributes
import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.SecurityLevel
import io.grpc.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.InputStream
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthCallCredentialsTest {

    private val authKey = AuthCallCredentials.AUTHORIZATION_METADATA_KEY

    private class FakeAuthTokenProvider(
        private var token: String?,
        private val refreshedToken: String? = null,
        private val shouldThrowOnRefresh: Throwable? = null,
    ) : AuthTokenProvider {
        var refreshCallCount = 0

        override fun getAccessToken(): String? = token
        override fun isAuthenticated(): Boolean = token != null

        override suspend fun refreshIfNeeded(): Result<String> {
            refreshCallCount++
            shouldThrowOnRefresh?.let { throw it }
            return if (refreshedToken != null) {
                token = refreshedToken
                Result.success(refreshedToken)
            } else {
                token?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No token"))
            }
        }
    }

    private class FakeMetadataApplier : CallCredentials.MetadataApplier() {
        var appliedHeaders: Metadata? = null
        var failedStatus: Status? = null

        override fun apply(headers: Metadata) {
            appliedHeaders = headers
        }

        override fun fail(status: Status) {
            failedStatus = status
        }
    }

    private class FakeRequestInfo : CallCredentials.RequestInfo() {
        override fun getMethodDescriptor(): MethodDescriptor<*, *> = dummyMethod()
        override fun getSecurityLevel(): SecurityLevel = SecurityLevel.NONE
        override fun getAuthority(): String = "localhost"
        override fun getTransportAttrs(): Attributes = Attributes.EMPTY
    }

    private val directExecutor = Executor { command -> command.run() }

    @Test
    fun `applyRequestMetadata attaches bearer token when token is available`() = runTest {
        val fakeProvider = FakeAuthTokenProvider(token = "jwt-valid-token-123")
        val dispatchers = TestCoroutineDispatchers(StandardTestDispatcher(testScheduler))
        val credentials = AuthCallCredentials(
            tokenProvider = fakeProvider,
            dispatchers = dispatchers,
            coroutineScope = this,
        )
        val applier = FakeMetadataApplier()

        credentials.applyRequestMetadata(FakeRequestInfo(), directExecutor, applier)
        testScheduler.advanceUntilIdle()

        val captured = applier.appliedHeaders?.get(authKey)
        assertEquals("Bearer jwt-valid-token-123", captured)
        assertNull(applier.failedStatus)
    }

    @Test
    fun `applyRequestMetadata refreshes token and attaches fresh bearer token`() = runTest {
        val fakeProvider = FakeAuthTokenProvider(
            token = "expired-token",
            refreshedToken = "fresh-jwt-token-789",
        )
        val dispatchers = TestCoroutineDispatchers(StandardTestDispatcher(testScheduler))
        val credentials = AuthCallCredentials(
            tokenProvider = fakeProvider,
            dispatchers = dispatchers,
            coroutineScope = this,
        )
        val applier = FakeMetadataApplier()

        credentials.applyRequestMetadata(FakeRequestInfo(), directExecutor, applier)
        testScheduler.advanceUntilIdle()

        val captured = applier.appliedHeaders?.get(authKey)
        assertEquals("Bearer fresh-jwt-token-789", captured)
        assertEquals(1, fakeProvider.refreshCallCount)
        assertNull(applier.failedStatus)
    }

    @Test
    fun `applyRequestMetadata attaches empty metadata when token is null`() = runTest {
        val fakeProvider = FakeAuthTokenProvider(token = null)
        val dispatchers = TestCoroutineDispatchers(StandardTestDispatcher(testScheduler))
        val credentials = AuthCallCredentials(
            tokenProvider = fakeProvider,
            dispatchers = dispatchers,
            coroutineScope = this,
        )
        val applier = FakeMetadataApplier()

        credentials.applyRequestMetadata(FakeRequestInfo(), directExecutor, applier)
        testScheduler.advanceUntilIdle()

        assertNotNull(applier.appliedHeaders)
        assertNull(applier.appliedHeaders?.get(authKey))
        assertNull(applier.failedStatus)
    }

    @Test
    fun `applyRequestMetadata attaches empty metadata when token is blank`() = runTest {
        val fakeProvider = FakeAuthTokenProvider(token = "   ")
        val dispatchers = TestCoroutineDispatchers(StandardTestDispatcher(testScheduler))
        val credentials = AuthCallCredentials(
            tokenProvider = fakeProvider,
            dispatchers = dispatchers,
            coroutineScope = this,
        )
        val applier = FakeMetadataApplier()

        credentials.applyRequestMetadata(FakeRequestInfo(), directExecutor, applier)
        testScheduler.advanceUntilIdle()

        assertNotNull(applier.appliedHeaders)
        assertNull(applier.appliedHeaders?.get(authKey))
        assertNull(applier.failedStatus)
    }

    @Test
    fun `applyRequestMetadata invokes applier fail when unexpected error occurs`() = runTest {
        val error = RuntimeException("OAuth server unreachable")
        val fakeProvider = FakeAuthTokenProvider(
            token = null,
            shouldThrowOnRefresh = error,
        )
        val dispatchers = TestCoroutineDispatchers(StandardTestDispatcher(testScheduler))
        val credentials = AuthCallCredentials(
            tokenProvider = fakeProvider,
            dispatchers = dispatchers,
            coroutineScope = this,
        )
        val applier = FakeMetadataApplier()

        credentials.applyRequestMetadata(FakeRequestInfo(), directExecutor, applier)
        testScheduler.advanceUntilIdle()

        assertNull(applier.appliedHeaders)
        assertNotNull(applier.failedStatus)
        assertEquals(Status.Code.UNAUTHENTICATED, applier.failedStatus?.code)
        assertEquals(error, applier.failedStatus?.cause)
    }

    @Test
    fun `applyRequestMetadata returns immediately without blocking caller`() = runTest {
        val fakeProvider = FakeAuthTokenProvider(token = "jwt-valid-token-123")
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestCoroutineDispatchers(testDispatcher)
        val credentials = AuthCallCredentials(
            tokenProvider = fakeProvider,
            dispatchers = dispatchers,
            coroutineScope = this,
        )
        val applier = FakeMetadataApplier()

        // Calling applyRequestMetadata does NOT block or resolve synchronously
        credentials.applyRequestMetadata(FakeRequestInfo(), directExecutor, applier)

        // Before advancing dispatcher, applier has NOT been called yet
        assertNull(applier.appliedHeaders)

        // After advancing scheduler, background coroutine executes and applies metadata
        testScheduler.advanceUntilIdle()
        assertNotNull(applier.appliedHeaders)
    }

    companion object {
        private fun dummyMethod(): MethodDescriptor<String, String> {
            val marshaller = object : MethodDescriptor.Marshaller<String> {
                override fun stream(value: String): InputStream = value.byteInputStream()
                override fun parse(stream: InputStream): String = stream.bufferedReader().readText()
            }
            return MethodDescriptor.newBuilder<String, String>()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("test/TestMethod")
                .setRequestMarshaller(marshaller)
                .setResponseMarshaller(marshaller)
                .build()
        }
    }
}
