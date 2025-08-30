package mk.ukim.finki.examscheduling.externalintegration.infrastructure

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ExternalServiceMetricsCollector(
    private val meterRegistry: MeterRegistry
) {
    private val logger = LoggerFactory.getLogger(ExternalServiceMetricsCollector::class.java)

    fun incrementAccreditationApiCall(endpoint: String) {
        Counter.builder("external.api.calls")
            .tag("service", "accreditation")
            .tag("endpoint", endpoint)
            .tag("status", "initiated")
            .register(meterRegistry)
            .increment()

        logger.debug("Incremented accreditation API call for endpoint: {}", endpoint)
    }

    fun incrementRaspredelbaApiCall(endpoint: String) {
        Counter.builder("external.api.calls")
            .tag("service", "raspredelba")
            .tag("endpoint", endpoint)
            .tag("status", "initiated")
            .register(meterRegistry)
            .increment()

        logger.debug("Incremented raspredelba API call for endpoint: {}", endpoint)
    }

    fun recordSuccessfulApiCall(serviceName: String, endpoint: String) {
        Counter.builder("external.api.calls")
            .tag("service", serviceName)
            .tag("endpoint", endpoint)
            .tag("status", "success")
            .register(meterRegistry)
            .increment()

        Timer.builder("external.api.response_time")
            .tag("service", serviceName)
            .tag("endpoint", endpoint)
            .register(meterRegistry)
    }

    fun recordFailedApiCall(serviceName: String, endpoint: String, exception: Exception) {
        Counter.builder("external.api.calls")
            .tag("service", serviceName)
            .tag("endpoint", endpoint)
            .tag("status", "failure")
            .tag("error_type", exception.javaClass.simpleName)
            .register(meterRegistry)
            .increment()

        logger.warn("Recorded failed API call for {}.{}: {}", serviceName, endpoint, exception.message)
    }

    fun incrementDataValidationFailure(dataType: String, identifier: String) {
        Counter.builder("data.validation.failures")
            .tag("data_type", dataType)
            .tag("identifier", identifier)
            .register(meterRegistry)
            .increment()

        logger.warn("Data validation failure for {} with identifier: {}", dataType, identifier)
    }

    fun incrementFallbackUsage(serviceName: String, endpoint: String) {
        Counter.builder("circuit_breaker.fallback")
            .tag("service", serviceName)
            .tag("endpoint", endpoint)
            .register(meterRegistry)
            .increment()

        logger.info("Circuit breaker fallback used for {}.{}", serviceName, endpoint)
    }

    fun recordCacheHit(cacheType: String, key: String) {
        Counter.builder("cache.hits")
            .tag("cache_type", cacheType)
            .register(meterRegistry)
            .increment()
    }

    fun recordCacheMiss(cacheType: String, key: String) {
        Counter.builder("cache.misses")
            .tag("cache_type", cacheType)
            .register(meterRegistry)
            .increment()
    }

    /*fun recordDataFreshnessMetric(dataType: String, ageInHours: Long) {
        Gauge.builder("data.freshness.hours")
            .tag("data_type", dataType)
            .register(meterRegistry)
    }*/
}
