package mk.ukim.finki.examscheduling.preferencemanagement.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {

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
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }

    @Bean("externalIntegrationWebClient")
    fun externalIntegrationWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(externalIntegrationBaseUrl)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }
}