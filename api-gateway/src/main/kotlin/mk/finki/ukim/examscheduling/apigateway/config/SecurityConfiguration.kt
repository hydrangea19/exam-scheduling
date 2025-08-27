package mk.finki.ukim.examscheduling.apigateway.config

import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtAuthenticationWebFilter
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration(
    private val jwtTokenService: JwtTokenService
) {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints
                    .pathMatchers("/api/auth/**").permitAll() // User management authentication
                    .pathMatchers("/test/**").permitAll() // Test endpoints
                    .pathMatchers("/api/gateway/ping").permitAll()
                    .pathMatchers("/api/gateway/auth-status").permitAll() // Gateway status check
                    .pathMatchers("/actuator/**").permitAll()

                    // Admin-only endpoints across all services
                    .pathMatchers("/api/admin/**").hasRole("ADMIN")
                    .pathMatchers("/api/users/**").hasRole("ADMIN")

                    // Professor and admin endpoints
                    .pathMatchers("/api/professor/**").hasAnyRole("ADMIN", "PROFESSOR")
                    .pathMatchers("/api/preferences/**").hasAnyRole("ADMIN", "PROFESSOR")
                    .pathMatchers("/api/scheduling/**").hasAnyRole("ADMIN", "PROFESSOR")
                    .pathMatchers("/api/publishing/**").hasAnyRole("ADMIN", "PROFESSOR")

                    // Public content
                    .pathMatchers("/api/public/**").permitAll()

                    // External integration endpoints
                    .pathMatchers("/api/external/**").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")

                    // Test and debugging endpoints
                    .pathMatchers("/api/test/**").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")

                    // All other requests require authentication
                    .anyExchange().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationWebFilter(jwtTokenService),
                SecurityWebFiltersOrder.AUTHENTICATION
            )
            .build()
    }
}