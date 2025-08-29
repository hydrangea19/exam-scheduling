package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class ChangeRoleRequest(
    val previousRole: String,
    val newRole: String,
    val changedBy: String,
    val reason: String? = null
)
