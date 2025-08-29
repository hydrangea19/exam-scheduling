package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class CreateEnhancedUserRequest(
    val email: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val role: String,
    val password: String? = null
)