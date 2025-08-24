package mk.ukim.finki.examscheduling.publishingservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration {

    @Value("\${external-services.scheduling.base-url}")
    private lateinit var schedulingBaseUrl: String

    @Value("\${external-services.user-management.base-url}")
    private lateinit var userManagementBaseUrl: String

    @Bean("schedulingWebClient")
    fun schedulingWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(schedulingBaseUrl)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }

    @Bean("userManagementWebClient")
    fun userManagementWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(userManagementBaseUrl)
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .build()
    }
}