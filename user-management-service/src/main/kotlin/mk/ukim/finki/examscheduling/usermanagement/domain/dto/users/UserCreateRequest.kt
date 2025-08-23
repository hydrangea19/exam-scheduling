package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class UserCreateRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null
)
