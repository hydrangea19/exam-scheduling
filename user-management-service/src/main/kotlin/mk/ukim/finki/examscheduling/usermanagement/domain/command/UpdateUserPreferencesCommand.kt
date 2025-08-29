package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class UpdateUserPreferencesCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val notificationPreferences: Map<String, Boolean>,
    val uiPreferences: Map<String, String>
)
