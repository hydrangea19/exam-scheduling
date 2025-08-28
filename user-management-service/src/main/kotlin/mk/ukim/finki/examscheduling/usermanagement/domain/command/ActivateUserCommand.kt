package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class ActivateUserCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val activatedBy: UUID? = null,
    val reason: String? = null
)
