package mk.ukim.finki.examscheduling.usermanagement.domain.command

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ChangeUserRoleCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val newRole: UserRole,
    val previousRole: UserRole,
    val changedBy: String,
    val reason: String? = null
)
