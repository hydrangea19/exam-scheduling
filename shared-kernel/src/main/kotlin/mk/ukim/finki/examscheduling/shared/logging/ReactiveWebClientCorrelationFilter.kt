package mk.ukim.finki.examscheduling.shared.logging

import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono

class ReactiveWebClientCorrelationFilter {

    companion object {
        fun create(): ExchangeFilterFunction {
            return ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
                val correlationId = CorrelationIdContext.getCorrelationId()
                val requestId = CorrelationIdContext.getRequestId()
                val userId = CorrelationIdContext.getUserId()

                val requestBuilder = ClientRequest.from(clientRequest)

                correlationId?.let {
                    requestBuilder.header(CorrelationIdContext.CORRELATION_ID_HEADER, it)
                }

                requestId?.let {
                    requestBuilder.header(CorrelationIdContext.REQUEST_ID_HEADER, it)
                }

                userId?.let {
                    requestBuilder.header(CorrelationIdContext.USER_ID_HEADER, it)
                }

                Mono.just(requestBuilder.build())
            }
        }
    }
}