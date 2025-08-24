package mk.finki.ukim.examscheduling.apigateway.config

import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtAuthenticationWebFilter
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenProvider

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration(
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/api/auth/**").permitAll()
                    .pathMatchers("/test/**").permitAll()
                    .pathMatchers("/api/gateway/ping").permitAll()
                    .pathMatchers("/actuator/**").permitAll()

                    .pathMatchers("/api/admin/**").hasRole("ADMIN")

                    .pathMatchers("/api/professor/**").hasAnyRole("ADMIN", "PROFESSOR")
                    .pathMatchers("/api/preferences/**").hasAnyRole("ADMIN", "PROFESSOR")

                    .pathMatchers("/api/scheduling/**").hasAnyRole("ADMIN", "PROFESSOR")
                    .pathMatchers("/api/publishing/**").hasAnyRole("ADMIN", "PROFESSOR")

                    .pathMatchers("/api/users/**").hasRole("ADMIN")

                    .pathMatchers("/api/external/**").hasAnyRole("ADMIN", "PROFESSOR")

                    .anyExchange().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationWebFilter(jwtTokenProvider),
                SecurityWebFiltersOrder.AUTHENTICATION
            )
            .build()
    }
}