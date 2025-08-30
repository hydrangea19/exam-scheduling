package mk.ukim.finki.examscheduling.externalintegration.config

import feign.RequestInterceptor
import feign.codec.ErrorDecoder
import mk.ukim.finki.examscheduling.externalintegration.domain.exceptions.ExternalServiceIntegrationException
import mk.ukim.finki.examscheduling.externalintegration.domain.exceptions.ServiceCircuitBreakerOpenException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RaspredelbaServiceClientConfiguration {

    @Bean
    fun raspredelbaRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            requestTemplate.header("User-Agent", "ExamScheduling-Service")
            requestTemplate.header("Accept", "application/json")

            val token = getCurrentServiceToken()
            if (token != null) {
                requestTemplate.header("Authorization", "Bearer $token")
            }
        }
    }

    @Bean
    fun raspredelbaErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            when (response.status()) {
                401 -> ExternalServiceIntegrationException("Authentication failed with Raspredelba service")
                404 -> ExternalServiceIntegrationException("Raspredelba data not found")
                503 -> ServiceCircuitBreakerOpenException("raspredelba")
                else -> ErrorDecoder.Default().decode(methodKey, response)
            }
        }
    }

    private fun getCurrentServiceToken(): String? {
        return "mock-service-token"
    }
}