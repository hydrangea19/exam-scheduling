package mk.ukim.finki.examscheduling.sharedsecurity.dto

data class AuthenticationRequest(
    val email: String,
    val password: String? = null,
    val loginType: String = "email"
)