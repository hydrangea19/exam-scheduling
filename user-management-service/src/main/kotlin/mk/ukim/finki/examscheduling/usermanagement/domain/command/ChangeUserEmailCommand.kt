package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class ChangeUserEmailCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val newEmail: String,
    val oldEmail: String
)
