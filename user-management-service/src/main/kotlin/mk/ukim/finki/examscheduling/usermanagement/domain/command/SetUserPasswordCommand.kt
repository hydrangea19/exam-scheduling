package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class SetUserPasswordCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val passwordHash: String
)
