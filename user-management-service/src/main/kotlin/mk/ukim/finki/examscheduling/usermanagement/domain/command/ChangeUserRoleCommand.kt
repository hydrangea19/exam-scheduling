package mk.ukim.finki.examscheduling.usermanagement.domain.command

import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class ChangeUserRoleCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val newRole: UserRole,
    val previousRole: UserRole,
    val changedBy: UUID,
    val reason: String? = null
)
