package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class UpdateUserProfileCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val firstName: String,
    val lastName: String,
    val middleName: String? = null
)
