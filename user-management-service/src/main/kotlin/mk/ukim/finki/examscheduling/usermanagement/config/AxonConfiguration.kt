package mk.ukim.finki.examscheduling.usermanagement.config

import com.fasterxml.jackson.databind.ObjectMapper
import mk.ukim.finki.examscheduling.usermanagement.domain.aggregate.UserAggregate
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
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
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
    fun userAggregateRepository(
        eventStore: EventStore,
        cache: Cache
    ): Repository<UserAggregate> {
        logger.info("Configuring UserAggregate repository")
        return EventSourcingRepository.builder(UserAggregate::class.java)
            .eventStore(eventStore)
            .cache(cache)
            .build()
    }

    @Bean
    fun axonHealthCheck(
        commandBus: CommandBus,
        eventBus: EventBus
    ): AxonHealthIndicator {
        return AxonHealthIndicator(commandBus, eventBus)
    }
}
