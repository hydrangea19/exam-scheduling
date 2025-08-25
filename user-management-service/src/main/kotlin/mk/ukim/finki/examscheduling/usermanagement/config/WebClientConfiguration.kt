package mk.ukim.finki.examscheduling.usermanagement.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mk.ukim.finki.examscheduling.shared.logging.ReactiveWebClientCorrelationFilter
import mk.ukim.finki.examscheduling.sharedsecurity.jwt.JwtTokenProvider
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfiguration(
    private val jwtTokenProvider: JwtTokenProvider
) {

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
            .filter(addJwtTokenFilter())
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
                        fullName = "System Service"
                    )
                }

                val modifiedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer $token")
                    .build()

                Mono.just(modifiedRequest)
            } catch (e: Exception) {
                println("Failed to add JWT token: ${e.message}")
                Mono.just(request)
            }
        }
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