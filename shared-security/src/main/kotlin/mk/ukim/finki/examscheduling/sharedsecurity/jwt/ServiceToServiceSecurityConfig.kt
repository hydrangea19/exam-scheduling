package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class ServiceToServiceSecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Bean
    fun serviceWebClient(): WebClient {
        return WebClient.builder()
            .filter(addJwtTokenFilter())
            .build()
    }

    private fun addJwtTokenFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            try {
                val currentUser = SecurityUtils.getCurrentUser()
                if (currentUser != null) {
                    val token = jwtTokenProvider.generateToken(
                        userId = currentUser.id,
                        email = currentUser.username,
                        role = currentUser.role,
                        fullName = currentUser.fullName
                    )

                    val modifiedRequest = ClientRequest.from(request)
                        .header("Authorization", "Bearer $token")
                        .build()

                    Mono.just(modifiedRequest)
                } else {
                    val systemToken = jwtTokenProvider.generateToken(
                        userId = "system",
                        email = "system@examscheduling.local",
                        role = "SYSTEM",
                        fullName = "System Service"
                    )

                    val modifiedRequest = ClientRequest.from(request)
                        .header("Authorization", "Bearer $systemToken")
                        .build()

                    Mono.just(modifiedRequest)
                }
            } catch (e: Exception) {
                println("Failed to add JWT token to service call: ${e.message}")
                Mono.just(request)
            }
        }
    }
}