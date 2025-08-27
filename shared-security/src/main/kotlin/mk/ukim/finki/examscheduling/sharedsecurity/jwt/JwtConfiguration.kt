package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@Configuration
class JwtConfiguration {

    private val logger = LoggerFactory.getLogger(JwtConfiguration::class.java)

    @Value("\${keycloak.server.url}")
    private lateinit var keycloakServerUrl: String

    @Value("\${keycloak.realm}")
    private lateinit var keycloakRealm: String

    @Value("\${authentication.keycloak.enabled:true}")
    private var keycloakEnabled: Boolean = true

    /**
     * JWT Decoder for Keycloak tokens
     * This will validate tokens issued by Keycloak using Keycloak's public keys
     */
    @Bean("keycloakJwtDecoder")
    fun keycloakJwtDecoder(): JwtDecoder? {
        return if (keycloakEnabled) {
            try {
                val jwkSetUri = "$keycloakServerUrl/realms/$keycloakRealm/protocol/openid-connect/certs"
                logger.info("Configuring Keycloak JWT decoder with JWK set URI: {}", jwkSetUri)

                NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                    .build()
            } catch (e: Exception) {
                logger.error("Failed to configure Keycloak JWT decoder", e)
                null
            }
        } else {
            logger.info("Keycloak JWT decoder is disabled")
            null
        }
    }

    /**
     * Default JWT Decoder - will be used by Spring Security OAuth2 Resource Server
     * We make this primary so it's used by default, but we can also inject keycloakJwtDecoder specifically
     */
    @Primary
    @Bean
    fun defaultJwtDecoder(): JwtDecoder? {
        return keycloakJwtDecoder()
    }
}