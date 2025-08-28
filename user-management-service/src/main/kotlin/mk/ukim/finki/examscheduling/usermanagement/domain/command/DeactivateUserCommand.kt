package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class DeactivateUserCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val deactivatedBy: UUID,
    val reason: String
)
