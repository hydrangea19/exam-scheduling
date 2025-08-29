package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class UpdateUserProfileCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null
)
