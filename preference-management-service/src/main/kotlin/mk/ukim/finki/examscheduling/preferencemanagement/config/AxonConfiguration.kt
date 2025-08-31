package mk.ukim.finki.examscheduling.preferencemanagement.config

import com.fasterxml.jackson.databind.ObjectMapper
import mk.ukim.finki.examscheduling.preferencemanagement.domain.aggregate.ExamSessionPeriodAggregate
import mk.ukim.finki.examscheduling.preferencemanagement.domain.aggregate.ProfessorPreferenceSubmissionAggregate
import org.axonframework.commandhandling.CommandBus
import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.WeakReferenceCache
import org.axonframework.eventhandling.EventBus
import org.axonframework.eventsourcing.EventSourcingRepository
import org.axonframework.eventsourcing.eventstore.EventStore
import org.axonframework.modelling.command.Repository
import org.axonframework.serialization.Serializer
import org.axonframework.serialization.json.JacksonSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.actuate.health.Status
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class AxonConfiguration {

    private val logger = LoggerFactory.getLogger(AxonConfiguration::class.java)

    @Bean
    @Primary
    fun serializer(objectMapper: ObjectMapper): Serializer {
        return JacksonSerializer.builder()
            .objectMapper(objectMapper)
            .build()
    }

    @Bean
    fun aggregateCache(): Cache {
        return WeakReferenceCache()
    }

    @Bean
    fun professorPreferenceSubmissionAggregateRepository(
        eventStore: EventStore,
        cache: Cache
    ): Repository<ProfessorPreferenceSubmissionAggregate> {
        logger.info("Configuring ProfessorPreferenceSubmissionAggregate repository")
        return EventSourcingRepository.builder(ProfessorPreferenceSubmissionAggregate::class.java)
            .eventStore(eventStore)
            .cache(cache)
            .build()
    }

    @Bean
    fun examSessionPeriodAggregateRepository(
        eventStore: EventStore,
        cache: Cache
    ): Repository<ExamSessionPeriodAggregate> {
        logger.info("Configuring ExamSessionPeriodAggregate repository")
        return EventSourcingRepository.builder(ExamSessionPeriodAggregate::class.java)
            .eventStore(eventStore)
            .cache(cache)
            .build()
    }

    @Bean
    fun axonHealthCheck(
        commandBus: CommandBus,
        eventBus: EventBus
    ): HealthIndicator {
        return HealthIndicator {
            try {
                val status = if (commandBus != null && eventBus != null) {
                    Status.UP
                } else {
                    Status.DOWN
                }

                org.springframework.boot.actuate.health.Health.status(status)
                    .withDetail("commandBus", if (commandBus != null) "Available" else "Not Available")
                    .withDetail("eventBus", if (eventBus != null) "Available" else "Not Available")
                    .withDetail(
                        "aggregates", listOf(
                            "ProfessorPreferenceSubmissionAggregate",
                            "ExamSessionPeriodAggregate"
                        )
                    )
                    .build()
            } catch (e: Exception) {
                logger.error("Axon health check failed", e)
                org.springframework.boot.actuate.health.Health.down()
                    .withDetail("error", e.message)
                    .build()
            }
        }
    }
}