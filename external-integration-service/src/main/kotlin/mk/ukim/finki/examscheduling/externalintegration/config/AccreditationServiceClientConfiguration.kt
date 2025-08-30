package mk.ukim.finki.examscheduling.externalintegration.config

import feign.RequestInterceptor
import feign.codec.ErrorDecoder
import mk.ukim.finki.examscheduling.externalintegration.domain.exceptions.ExternalServiceIntegrationException
import mk.ukim.finki.examscheduling.externalintegration.domain.exceptions.ServiceCircuitBreakerOpenException
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AccreditationServiceClientConfiguration {

    @Bean
    fun accreditationRequestInterceptor(): RequestInterceptor {
        return RequestInterceptor { requestTemplate ->
            requestTemplate.header("User-Agent", "ExamScheduling-Service")
            requestTemplate.header("Accept", "application/json")
        }
    }

    @Bean
    fun accreditationErrorDecoder(): ErrorDecoder {
        return ErrorDecoder { methodKey, response ->
            when (response.status()) {
                404 -> ExternalServiceIntegrationException("Accreditation data not found")
                503 -> ServiceCircuitBreakerOpenException("accreditation")
                else -> ErrorDecoder.Default().decode(methodKey, response)
            }
        }
    }
}