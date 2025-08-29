package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class ActivateUserRequest(
    val activatedBy: String? = null,
    val reason: String? = null
)
