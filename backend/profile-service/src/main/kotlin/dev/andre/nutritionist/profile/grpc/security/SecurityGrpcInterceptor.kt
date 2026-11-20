package dev.andre.nutritionist.profile.grpc.security

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.stereotype.Component
import java.util.Base64

/**
 * Серверный интерцептор безопасности gRPC (T-021).
 *
 * Реализует Defense-in-Depth аутентификацию:
 * 1. Считывает заголовок `x-user-id` и `x-user-roles`, прокинутые Envoy Gateway после валидации JWT.
 * 2. При отсутствии заголовков шлюза — производит fallback на Bearer JWT токен из заголовка `authorization`.
 * 3. Формирует [AuthContext] и помещает его в gRPC [Context], делая доступным в Kotlin Coroutines.
 * 4. Записывает `userId` в [MDC] для структурированного JSON-логирования.
 */
@Component
@GlobalServerInterceptor
@Order(SecurityGrpcInterceptor.ORDER_PRIORITY)
class SecurityGrpcInterceptor(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : ServerInterceptor {
    private val logger = LoggerFactory.getLogger(SecurityGrpcInterceptor::class.java)

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val authContext = extractAuthContext(headers)

        val previousMdcUserId = MDC.get(MDC_USER_ID_KEY)
        if (authContext.isAuthenticated && !authContext.userId.isNullOrBlank()) {
            MDC.put(MDC_USER_ID_KEY, authContext.userId)
        }

        val grpcContext = Context.current().withValue(AuthContext.AUTH_CONTEXT_KEY, authContext)
        val listener = Contexts.interceptCall(grpcContext, call, headers, next)

        return object : io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onComplete() {
                try {
                    super.onComplete()
                } finally {
                    cleanMdc(previousMdcUserId)
                }
            }

            override fun onCancel() {
                try {
                    super.onCancel()
                } finally {
                    cleanMdc(previousMdcUserId)
                }
            }
        }
    }

    private fun cleanMdc(previousMdcUserId: String?) {
        if (previousMdcUserId != null) {
            MDC.put(MDC_USER_ID_KEY, previousMdcUserId)
        } else {
            MDC.remove(MDC_USER_ID_KEY)
        }
    }

    private fun extractAuthContext(headers: Metadata): AuthContext {
        val gatewayUserId = headers.get(X_USER_ID_HEADER)?.trim()
        if (!gatewayUserId.isNullOrBlank()) {
            return AuthContext(
                userId = gatewayUserId,
                roles = parseRolesFromHeader(headers.get(X_USER_ROLES_HEADER)),
                isAuthenticated = true,
            )
        }

        val authHeader = headers.get(AUTHORIZATION_HEADER)?.trim().orEmpty()
        val bearerToken =
            if (authHeader.startsWith(BEARER_PREFIX, ignoreCase = true)) {
                authHeader.substring(BEARER_PREFIX.length).trim()
            } else {
                null
            }

        return bearerToken?.let { parseJwtToken(it) } ?: AuthContext.UNAUTHENTICATED
    }

    private fun parseRolesFromHeader(rolesHeader: String?): Set<String> {
        val trimmed = rolesHeader?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return setOf(AuthContext.ROLE_USER)
        }
        val roles =
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                parseJsonRoles(trimmed)
            } else {
                trimmed
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }
        return roles.ifEmpty { setOf(AuthContext.ROLE_USER) }
    }

    private fun parseJsonRoles(json: String): Set<String> =
        runCatching {
            val node = objectMapper.readTree(json)
            if (node.isArray) {
                node
                    .mapNotNull { it.asText() }
                    .filter { it.isNotBlank() }
                    .toSet()
            } else {
                setOf(AuthContext.ROLE_USER)
            }
        }.getOrDefault(setOf(AuthContext.ROLE_USER))

    private fun parseJwtToken(token: String): AuthContext? =
        runCatching {
            val parts = token.split(".")
            if (parts.size != JWT_PARTS_COUNT) {
                logger.warn("Malformed JWT token: expected {} parts, got {}", JWT_PARTS_COUNT, parts.size)
                null
            } else {
                val payloadBytes = Base64.getUrlDecoder().decode(parts[1])
                val rootNode: JsonNode = objectMapper.readTree(payloadBytes)
                val userId = rootNode.get("sub")?.asText()?.takeIf { it.isNotBlank() }

                if (userId == null) {
                    null
                } else {
                    val roles = mutableSetOf<String>()
                    val realmAccess = rootNode.path("realm_access").path("roles")
                    if (realmAccess.isArray) {
                        realmAccess.forEach { roles.add(it.asText()) }
                    }
                    val directRoles = rootNode.path("roles")
                    if (directRoles.isArray) {
                        directRoles.forEach { roles.add(it.asText()) }
                    }
                    if (roles.isEmpty()) {
                        roles.add(AuthContext.ROLE_USER)
                    }
                    AuthContext(userId = userId, roles = roles, isAuthenticated = true)
                }
            }
        }.onFailure { ex ->
            logger.warn("Failed to parse JWT payload: {}", ex.message)
        }.getOrNull()

    companion object {
        const val ORDER_PRIORITY: Int = Ordered.HIGHEST_PRECEDENCE + 20
        const val JWT_PARTS_COUNT: Int = 3
        const val MDC_USER_ID_KEY = "userId"
        const val BEARER_PREFIX = "Bearer "

        val X_USER_ID_HEADER: Metadata.Key<String> =
            Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER)

        val X_USER_ROLES_HEADER: Metadata.Key<String> =
            Metadata.Key.of("x-user-roles", Metadata.ASCII_STRING_MARSHALLER)

        val AUTHORIZATION_HEADER: Metadata.Key<String> =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
    }
}
