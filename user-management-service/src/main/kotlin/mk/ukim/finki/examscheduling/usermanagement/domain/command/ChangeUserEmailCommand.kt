package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ChangeUserEmailCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val newEmail: String,
    val oldEmail: String
)
