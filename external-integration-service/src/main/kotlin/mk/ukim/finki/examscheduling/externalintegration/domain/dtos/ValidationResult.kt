package mk.ukim.finki.examscheduling.externalintegration.domain.dtos

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)