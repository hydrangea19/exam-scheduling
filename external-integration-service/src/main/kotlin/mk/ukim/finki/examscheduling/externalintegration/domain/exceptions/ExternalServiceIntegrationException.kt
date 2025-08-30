package mk.ukim.finki.examscheduling.externalintegration.domain.exceptions

class ExternalServiceIntegrationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)