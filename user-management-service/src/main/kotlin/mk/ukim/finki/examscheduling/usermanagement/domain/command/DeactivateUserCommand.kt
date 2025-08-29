package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class DeactivateUserCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val deactivatedBy: String,
    val reason: String
)
