package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val middleName: String? = null
)