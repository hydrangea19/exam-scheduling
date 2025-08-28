package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import mk.ukim.finki.examscheduling.sharedsecurity.domain.UserPrincipal
import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.TokenValidationResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthenticationWebFilter(
    private val jwtTokenService: JwtTokenService
) : WebFilter {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationWebFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.value()
        val method = exchange.request.method

        if (method?.name() == "OPTIONS") {
            logger.info("JWT Filter - Skipping OPTIONS request for CORS")
            return chain.filter(exchange)
        }

        if (isAuthPath(path) || isPublicPath(path)) {
            return chain.filter(exchange)
        }

        val token = extractToken(exchange)

        return if (token != null) {
            val validationResult = jwtTokenService.validateToken(token)

            if (validationResult is TokenValidationResult.Valid) {
                val subject = validationResult.subject ?: validationResult.email
                val role = validationResult.role ?: "GUEST"
                val email = validationResult.email

                if (subject != null && email != null) {
                    val userPrincipal = UserPrincipal(
                        id = subject,
                        email = email,
                        role = role,
                        fullName = validationResult.fullName
                    )

                    val authentication = UsernamePasswordAuthenticationToken(
                        userPrincipal,
                        null,
                        userPrincipal.authorities
                    )

                    chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                } else {
                    logger.warn("Valid token is missing required claims (subject/email).")
                    handleUnauthorized(exchange)
                }
            } else {
                logger.warn("Received an invalid token. Reason: ${validationResult.error}")
                handleUnauthorized(exchange)
            }
        } else {
            logger.warn("Authorization header missing. Rejecting request.")
            handleUnauthorized(exchange)
        }
    }

    private fun extractToken(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        return if (StringUtils.hasText(authHeader) && authHeader!!.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            null
        }
    }

    private fun isPublicPath(path: String): Boolean {
        val publicPaths = listOf(
            "/api/auth/",
            "/api/auth",
            "/api/test/ping",
            "/actuator/",
            "/api/gateway/"
        )
        return publicPaths.any { publicPath -> path.startsWith(publicPath) }
    }

    private fun isAuthPath(path: String): Boolean {
        val authPaths = listOf(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/validate",
            "/api/auth/me"
        )
        val isAuth = authPaths.any { authPath -> path.startsWith(authPath) }
        if (isAuth) {
            logger.info("JWT Filter - Path '$path' identified as auth path")
        }
        return isAuth
    }


    private fun handleUnauthorized(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")
        val body = """{"error": "Unauthorized", "message": "Valid JWT token required"}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }
}