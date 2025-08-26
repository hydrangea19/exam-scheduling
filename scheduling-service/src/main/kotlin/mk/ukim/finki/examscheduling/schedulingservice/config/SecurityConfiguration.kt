package mk.ukim.finki.examscheduling.schedulingservice.config

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

@Configuration
@EnableWebSecurity
@Import(ServiceToServiceSecurityConfig::class)
class SecurityConfiguration(
    private val jwtTokenService: JwtTokenService
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/test/ping").permitAll()
                    .requestMatchers("/actuator/**").permitAll()

                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    .requestMatchers("/api/professor/**").hasAnyRole("ADMIN", "PROFESSOR")
                    .requestMatchers("/api/scheduling/**").hasAnyRole("ADMIN", "PROFESSOR")

                    .requestMatchers("/api/test/**").hasAnyRole("ADMIN", "PROFESSOR", "SYSTEM")

                    .anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenService),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }
}