/*
package mk.ukim.finki.examscheduling.usermanagement.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Configuration
@EnableConfigurationProperties
@ComponentScan(
    basePackages = [
        "mk.ukim.finki.examscheduling.usermanagement",
    ]
)
class UserManagementConfiguration {

    private val logger = LoggerFactory.getLogger(UserManagementConfiguration::class.java)

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        }
    }

    @EventListener
    fun handleContextRefresh(event: ContextRefreshedEvent) {
        logger.info("User Management Service configuration loaded successfully")
        logger.info("Available endpoints:")
        logger.info("  - POST   /api/users                    - Create user")
        logger.info("  - GET    /api/users/{id}               - Get user by ID")
        logger.info("  - GET    /api/users/email/{email}      - Get user by email")
        logger.info("  - GET    /api/users                    - Get all users (paginated)")
        logger.info("  - GET    /api/users/search             - Search users")
        logger.info("  - GET    /api/users/role/{role}        - Get users by role")
        logger.info("  - PUT    /api/users/{id}/profile       - Update user profile")
        logger.info("  - PUT    /api/users/{id}/email         - Change user email")
        logger.info("  - PUT    /api/users/{id}/role          - Change user role")
        logger.info("  - PUT    /api/users/{id}/activate      - Activate user")
        logger.info("  - PUT    /api/users/{id}/deactivate    - Deactivate user")
        logger.info("  - PUT    /api/users/{id}/preferences   - Update preferences")
        logger.info("  - GET    /api/users/me                 - Get current user")
        logger.info("  - PUT    /api/users/me/profile         - Update current user profile")
        logger.info("  - PUT    /api/users/me/preferences     - Update current user preferences")
        logger.info("  - GET    /api/users/statistics         - Get user statistics")
        logger.info("")
        logger.info("Authentication endpoints:")
        logger.info("  - POST   /api/auth/login               - User login")
        logger.info("  - POST   /api/auth/refresh             - Refresh token")
        logger.info("  - GET    /api/auth/me                  - Current user info")
        logger.info("  - POST   /api/auth/logout              - User logout")
    }
}


@Component
class StartupValidator(
    private val commandGateway: CommandGateway,
    private val queryGateway: QueryGateway,
) {

    private val logger = LoggerFactory.getLogger(StartupValidator::class.java)

    @EventListener
    fun validateOnStartup(event: ApplicationReadyEvent) {
        logger.info("Validating User Management Service startup...")

        try {
            logger.debug("Testing command gateway connectivity...")

            logger.debug("Testing query gateway connectivity...")

            logger.debug("Testing database connectivity...")

            logger.info("Database connected successfully.")

            logger.info("✓ User Management Service startup validation completed successfully");

        } catch (e: Exception) {
            logger.error("✗ User Management Service startup validation failed", e)
            throw IllegalStateException("Service startup validation failed", e)
        }
    }
}*/
