package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class DeactivateUserRequest(
    val deactivatedBy: String,
    val reason: String
)
