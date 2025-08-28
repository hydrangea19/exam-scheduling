package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.util.*

data class SynchronizeUserWithKeycloakCommand(
    @TargetAggregateIdentifier
    val userId: UUID,
    val keycloakUserId: String,
    val keycloakUserInfo: Map<String, Any>
)
