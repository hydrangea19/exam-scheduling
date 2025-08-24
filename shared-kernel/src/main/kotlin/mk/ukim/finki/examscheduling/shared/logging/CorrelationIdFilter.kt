package mk.ukim.finki.examscheduling.shared.logging

import jakarta.annotation.PostConstruct
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class CorrelationIdFilter : Filter {

    private val logger = LoggerFactory.getLogger(CorrelationIdFilter::class.java)

    @Value("\${spring.application.name:unknown-service}")
    private lateinit var serviceName: String

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            val httpRequest = request
            val httpResponse = response

            try {
                val correlationId = httpRequest.getHeader(CorrelationIdContext.CORRELATION_ID_HEADER)
                    ?: CorrelationIdContext.generateCorrelationId()

                val requestId = CorrelationIdContext.generateRequestId()

                val userId = httpRequest.getHeader(CorrelationIdContext.USER_ID_HEADER)

                CorrelationIdContext.setCorrelationId(correlationId)
                CorrelationIdContext.setRequestId(requestId)
                CorrelationIdContext.setUserId(userId)
                CorrelationIdContext.setServiceName(serviceName)

                httpResponse.setHeader(CorrelationIdContext.CORRELATION_ID_HEADER, correlationId)
                httpResponse.setHeader(CorrelationIdContext.REQUEST_ID_HEADER, requestId)

                logger.debug(
                    "Processing request with correlationId: {}, requestId: {}, path: {}",
                    correlationId, requestId, httpRequest.requestURI
                )

                chain.doFilter(request, response)

            } finally {
                CorrelationIdContext.clear()
            }
        } else {
            chain.doFilter(request, response)
        }
    }

    @PostConstruct
    fun init() {
        println("CorrelationIdFilter initialized for service: $serviceName")
    }
}