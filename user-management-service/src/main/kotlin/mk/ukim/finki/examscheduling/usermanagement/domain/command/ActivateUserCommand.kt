package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ActivateUserCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val activatedBy: String? = null,
    val reason: String? = null
)
