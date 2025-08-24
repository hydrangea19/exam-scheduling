package mk.ukim.finki.examscheduling.usermanagement.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mk.ukim.finki.examscheduling.shared.logging.ReactiveWebClientCorrelationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfiguration {

    @Value("\${external-services.external-integration.base-url}")
    private lateinit var externalIntegrationBaseUrl: String

    @Value("\${external-services.external-integration.timeout:5000}")
    private var timeoutMillis: Int = 5000

    private fun createHttpClient(): HttpClient {
        return HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMillis)
            .responseTimeout(Duration.ofMillis(timeoutMillis.toLong()))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(timeoutMillis.toLong(), TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(timeoutMillis.toLong(), TimeUnit.MILLISECONDS))
            }
    }

    @Bean("externalIntegrationWebClient")
    fun externalIntegrationWebClient(webClientBuilder: WebClient.Builder): WebClient {
        val httpClient = createHttpClient()

        return webClientBuilder
            .baseUrl(externalIntegrationBaseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
            }
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("User-Agent", "user-management-service/1.2B")
            .filter(ReactiveWebClientCorrelationFilter.create())
            .build()
    }

    @Bean
    fun commonWebClient(webClientBuilder: WebClient.Builder): WebClient {
        val httpClient = createHttpClient()

        return webClientBuilder
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                configurer.defaultCodecs().maxInMemorySize(1024 * 1024)
            }
            .defaultHeader("Content-Type", "application/json")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("User-Agent", "user-management-service/1.2B")
            .build()
    }
}