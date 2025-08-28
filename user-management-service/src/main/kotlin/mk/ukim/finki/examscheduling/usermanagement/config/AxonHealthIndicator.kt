package mk.ukim.finki.examscheduling.usermanagement.config

import org.axonframework.commandhandling.CommandBus
import org.axonframework.eventhandling.EventBus
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class AxonHealthIndicator(
    private val commandBus: CommandBus,
    private val eventBus: EventBus
) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(AxonHealthIndicator::class.java)

    override fun health(): Health {
        return try {
            val commandBusHealthy = commandBus != null
            val eventBusHealthy = eventBus != null

            if (commandBusHealthy && eventBusHealthy) {
                logger.debug("Axon Framework components are healthy")
                Health.up()
                    .withDetail("commandBus", "connected")
                    .withDetail("eventBus", "connected")
                    .build()
            } else {
                Health.down()
                    .withDetail("commandBus", if (commandBusHealthy) "connected" else "disconnected")
                    .withDetail("eventBus", if (eventBusHealthy) "connected" else "disconnected")
                    .build()
            }
        } catch (e: Exception) {
            logger.error("Axon health check failed", e)
            Health.down()
                .withException(e)
                .build()
        }
    }
}