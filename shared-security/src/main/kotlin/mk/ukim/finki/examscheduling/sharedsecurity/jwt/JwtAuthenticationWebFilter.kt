package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthenticationWebFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : WebFilter {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationWebFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.path.pathWithinApplication().value()

        if (isPublicPath(path)) {
            return chain.filter(exchange)
        }

        val token = extractToken(exchange)

        return if (token != null && jwtTokenProvider.validateToken(token)) {
            authenticateToken(token)
                .flatMap { authentication ->
                    chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                }
                .doOnNext {
                    logger.debug("Successfully authenticated request to: {}", path)
                }
        } else {
            logger.warn("Invalid or missing JWT token for path: {}", path)
            handleUnauthorized(exchange)
        }
    }

    private fun isPublicPath(path: String): Boolean {
        val publicPaths = listOf(
            "/api/auth/",
            "/api/auth",
            "/test/",
            "/actuator/",
            "/api/gateway/",
            "/api/gateway"
        )

        return publicPaths.any { publicPath ->
            path.startsWith(publicPath)
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

    private fun authenticateToken(token: String): Mono<UsernamePasswordAuthenticationToken> {
        return Mono.fromCallable {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val email = jwtTokenProvider.getEmailFromToken(token)
            val role = jwtTokenProvider.getRoleFromToken(token)
            val fullName = jwtTokenProvider.getFullNameFromToken(token)

            if (userId != null && email != null && role != null) {
                val userPrincipal = UserPrincipal(
                    id = userId,
                    email = email,
                    role = role,
                    fullName = fullName
                )

                val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))

                UsernamePasswordAuthenticationToken(userPrincipal, null, authorities)
            } else {
                throw IllegalArgumentException("Invalid token claims")
            }
        }
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