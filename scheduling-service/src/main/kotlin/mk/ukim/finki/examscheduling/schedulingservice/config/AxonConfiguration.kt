package mk.ukim.finki.examscheduling.schedulingservice.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mk.ukim.finki.examscheduling.schedulingservice.domain.aggregate.ExamSessionScheduleAggregate
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
    fun examSessionScheduleAggregateRepository(
        eventStore: EventStore,
        cache: Cache
    ): Repository<ExamSessionScheduleAggregate> {
        logger.info("Configuring ExamSessionScheduleAggregate repository")
        return EventSourcingRepository.builder(ExamSessionScheduleAggregate::class.java)
            .eventStore(eventStore)
            .cache(cache)
            .build()
    }

   /* @Bean
    fun axonHealthCheck(
        commandBus: CommandBus,
        eventBus: EventBus
    ): AxonHealthIndicator {
        return AxonHealthIndicator(commandBus, eventBus)
    }*/

    /**
     * Customize object mapper for Axon serialization to handle specific types
     */
    @Bean
    fun axonObjectMapper(): ObjectMapper {
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule.Builder().build())
        mapper.registerModule(JavaTimeModule())
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        return mapper
    }
}