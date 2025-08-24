package mk.ukim.finki.examscheduling.shared.logging

import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

class WebClientCorrelationInterceptor : ClientHttpRequestInterceptor {

    private val logger = LoggerFactory.getLogger(WebClientCorrelationInterceptor::class.java)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {

        val correlationId = CorrelationIdContext.getCorrelationId()
        val requestId = CorrelationIdContext.getRequestId()
        val userId = CorrelationIdContext.getUserId()


        correlationId?.let {
            request.headers.set(CorrelationIdContext.CORRELATION_ID_HEADER, it)
        }

        requestId?.let {
            request.headers.set(CorrelationIdContext.REQUEST_ID_HEADER, it)
        }

        userId?.let {
            request.headers.set(CorrelationIdContext.USER_ID_HEADER, it)
        }

        logger.debug(
            "Outgoing HTTP request to {} with correlationId: {}, requestId: {}",
            request.uri, correlationId, requestId
        )

        return execution.execute(request, body)
    }
}