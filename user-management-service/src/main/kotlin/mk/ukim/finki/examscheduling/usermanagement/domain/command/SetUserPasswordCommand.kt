package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class SetUserPasswordCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val passwordHash: String
)
