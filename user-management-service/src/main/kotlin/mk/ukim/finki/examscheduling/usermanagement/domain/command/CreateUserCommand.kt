package mk.ukim.finki.examscheduling.usermanagement.domain.command

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class CreateUserCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null,
    val role: UserRole,
    val passwordHash: String? = null
)
