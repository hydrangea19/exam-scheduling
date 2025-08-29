package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

data class ChangeEmailRequest(
    val oldEmail: String,
    val newEmail: String
)