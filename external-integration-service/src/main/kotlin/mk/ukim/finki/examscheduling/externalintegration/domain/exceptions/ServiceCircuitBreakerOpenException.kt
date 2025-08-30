package mk.ukim.finki.examscheduling.externalintegration.domain.exceptions

class ServiceCircuitBreakerOpenException(serviceName: String) :
    Exception("Circuit breaker is open for service: $serviceName")