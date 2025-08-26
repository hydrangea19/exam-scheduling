package mk.ukim.finki.examscheduling.sharedsecurity.dto

data class UserInfo(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)