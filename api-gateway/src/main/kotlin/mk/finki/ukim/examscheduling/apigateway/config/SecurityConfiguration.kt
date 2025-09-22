package mk.finki.ukim.examscheduling.apigateway.config

import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtAuthenticationWebFilter
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@Order(1)
class SecurityConfiguration(
    private val jwtTokenService: JwtTokenService
) {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .cors { }
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    .pathMatchers("/api/auth/login").permitAll()
                    .pathMatchers("/api/auth/refresh").permitAll()
                    .pathMatchers("/api/auth/logout").permitAll()
                    .pathMatchers("/test/**").permitAll() // Test endpoints
                    .pathMatchers("/api/gateway/ping").permitAll()
                    .pathMatchers("/api/gateway/auth-status").permitAll() // Gateway status check
                    .pathMatchers("/actuator/**").permitAll()

                    // Admin-only endpoints across all services
                    .pathMatchers("/api/admin/**").hasRole("ADMIN")
                    .pathMatchers("/api/users/**").hasAnyRole("ADMIN", "PROFESSOR")

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