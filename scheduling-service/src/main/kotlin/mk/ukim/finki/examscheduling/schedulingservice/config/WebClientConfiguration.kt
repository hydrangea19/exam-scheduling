package mk.ukim.finki.examscheduling.schedulingservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {

    @Value("\${external-services.preference-management.base-url}")
    private lateinit var preferenceManagementBaseUrl: String

    @Value("\${external-services.external-integration.base-url}")
    private lateinit var externalIntegrationBaseUrl: String

    @Bean("preferenceManagementWebClient")
    fun preferenceManagementWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(preferenceManagementBaseUrl)
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