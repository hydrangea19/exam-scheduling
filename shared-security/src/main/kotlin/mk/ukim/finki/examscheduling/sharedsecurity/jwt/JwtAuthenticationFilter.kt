package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mk.ukim.finki.examscheduling.sharedsecurity.domain.UserPrincipal
import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.TokenValidationResult
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtTokenService: JwtTokenService
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = getJwtFromRequest(request)

            if (jwt != null) {
                val validationResult = jwtTokenService.validateToken(jwt)

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

                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication

                        logger.debug("Set authentication for user: ${email} with role: ${role}")
                    } else {
                        logger.warn("Valid token is missing required claims (subject/email)")
                    }
                } else if (validationResult is TokenValidationResult.Invalid) {
                    logger.warn("Invalid token received. Reason: ${validationResult.reason}")
                }
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication in security context", ex)
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}