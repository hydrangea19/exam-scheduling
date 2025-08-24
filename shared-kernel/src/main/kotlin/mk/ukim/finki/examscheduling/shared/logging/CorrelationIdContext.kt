package mk.ukim.finki.examscheduling.shared.logging

import org.slf4j.MDC
import java.util.*

object CorrelationIdContext {
    const val CORRELATION_ID_HEADER = "X-Correlation-ID"
    const val REQUEST_ID_HEADER = "X-Request-ID"
    const val USER_ID_HEADER = "X-User-ID"

    private const val CORRELATION_ID_KEY = "correlationId"
    private const val REQUEST_ID_KEY = "requestId"
    private const val USER_ID_KEY = "userId"
    private const val SERVICE_NAME_KEY = "serviceName"

    fun generateCorrelationId(): String {
        return UUID.randomUUID().toString()
    }

    fun generateRequestId(): String {
        return UUID.randomUUID().toString()
    }

    fun setCorrelationId(correlationId: String) {
        MDC.put(CORRELATION_ID_KEY, correlationId)
    }

    fun getCorrelationId(): String? {
        return MDC.get(CORRELATION_ID_KEY)
    }

    fun setRequestId(requestId: String) {
        MDC.put(REQUEST_ID_KEY, requestId)
    }

    fun getRequestId(): String? {
        return MDC.get(REQUEST_ID_KEY)
    }

    fun setUserId(userId: String?) {
        userId?.let { MDC.put(USER_ID_KEY, it) }
    }

    fun getUserId(): String? {
        return MDC.get(USER_ID_KEY)
    }

    fun setServiceName(serviceName: String) {
        MDC.put(SERVICE_NAME_KEY, serviceName)
    }

    fun clear() {
        MDC.clear()
    }

    fun clearCorrelationId() {
        MDC.remove(CORRELATION_ID_KEY)
    }

    fun clearRequestId() {
        MDC.remove(REQUEST_ID_KEY)
    }

    fun clearUserId() {
        MDC.remove(USER_ID_KEY)
    }

    fun <T> withCorrelationId(correlationId: String, block: () -> T): T {
        val previousCorrelationId = getCorrelationId()
        try {
            setCorrelationId(correlationId)
            return block()
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId)
            } else {
                clearCorrelationId()
            }
        }
    }
}