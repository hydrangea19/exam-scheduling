package mk.ukim.finki.examscheduling.usermanagement.domain.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class SynchronizeUserWithKeycloakCommand(
    @TargetAggregateIdentifier
    val userId: String,
    val keycloakUserId: String,
    val keycloakUserInfo: Map<String, Any>
)
