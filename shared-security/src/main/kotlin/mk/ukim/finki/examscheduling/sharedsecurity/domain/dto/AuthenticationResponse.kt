package mk.ukim.finki.examscheduling.sharedsecurity.dto

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserInfo
)
