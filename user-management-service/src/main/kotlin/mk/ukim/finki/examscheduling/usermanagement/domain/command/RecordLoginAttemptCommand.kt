package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class RecordLoginAttemptCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val successful: Boolean,
    val ipAddress: String,
    val userAgent: String,
    val timestamp: Date = Date()
)
