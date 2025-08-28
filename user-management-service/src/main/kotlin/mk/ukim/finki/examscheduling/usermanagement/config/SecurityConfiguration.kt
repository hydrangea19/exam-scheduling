package mk.ukim.finki.examscheduling.usermanagement.config

import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtAuthenticationFilter
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenService
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.ServiceToServiceSecurityConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@Import(ServiceToServiceSecurityConfig::class)
class SecurityConfiguration(
    private val jwtTokenService: JwtTokenService,
    private val corsConfigurationSource: CorsConfigurationSource
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { it.configurationSource(corsConfigurationSource) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/test/ping").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/api/test/axon/**").permitAll()

                    .requestMatchers("/api/test/test-hybrid-authentication").permitAll()
                    .requestMatchers("/api/test/test-user-sync-stats").permitAll()

                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/users/**").hasRole("ADMIN")

                    .requestMatchers("/api/professor/**").hasAnyRole("ADMIN", "PROFESSOR")

                    .requestMatchers("/api/test/test-external-service").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")
                    .requestMatchers("/api/test/test-correlation-flow").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")
                    .requestMatchers("/api/test/test-logging-chain/**").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")
                    .requestMatchers("/api/test/test-structured-logging").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")
                    .requestMatchers("/api/test/test-full-integration").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")
                    .requestMatchers("/api/test/users/**").hasAnyRole("ADMIN", "SYSTEM")

                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenService),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}