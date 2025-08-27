package mk.ukim.finki.examscheduling.sharedsecurity.dto

data class TokenValidationResponse(
    val valid: Boolean,
    val expired: Boolean = false,
    val user: UserInfo? = null,
    val error: String? = null
)
