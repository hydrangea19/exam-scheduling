package mk.ukim.finki.examscheduling.externalintegration.domain.exceptions

class DataQualityValidationException(message: String, val errors: List<String>) :
    Exception(message)