package mk.ukim.finki.examscheduling.preferencemanagement.config

import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenProvider
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class WebClientConfiguration(
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Value("\${external-services.user-management.base-url}")
    private lateinit var userManagementBaseUrl: String

    @Value("\${external-services.user-management.timeout:5000}")
    private var userManagementTimeout: Long = 5000

    @Value("\${external-services.external-integration.base-url}")
    private lateinit var externalIntegrationBaseUrl: String

    @Value("\${external-services.external-integration.timeout:5000}")
    private var externalIntegrationTimeout: Long = 5000

    @Bean("userManagementWebClient")
    fun userManagementWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(userManagementBaseUrl)
            .filter(addJwtTokenFilter())
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }

    private fun addJwtTokenFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { request ->
            try {
                val currentUser = SecurityUtils.getCurrentUser()
                val token = if (currentUser != null) {
                    jwtTokenProvider.generateToken(
                        userId = currentUser.id,
                        email = currentUser.username,
                        role = currentUser.role,
                        fullName = currentUser.fullName
                    )
                } else {
                    jwtTokenProvider.generateToken(
                        userId = "system",
                        email = "system@examscheduling.local",
                        role = "SYSTEM",
                        fullName = "Preference Management System"
                    )
                }

                val modifiedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer $token")
                    .build()

                Mono.just(modifiedRequest)
            } catch (e: Exception) {
                println("Failed to add JWT token to service call from preference-management: ${e.message}")
                Mono.just(request)
            }
        }
    }

    @Bean("externalIntegrationWebClient")
    fun externalIntegrationWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(externalIntegrationBaseUrl)
            .filter(addJwtTokenFilter())
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }
}