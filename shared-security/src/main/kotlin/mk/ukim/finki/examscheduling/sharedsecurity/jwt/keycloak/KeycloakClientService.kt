package mk.ukim.finki.examscheduling.sharedsecurity.jwt.keycloak

import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakTokenIntrospection
import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakTokenResponse
import mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak.KeycloakUserInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class KeycloakClientService(
    private val webClient: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(KeycloakClientService::class.java)

    @Value("\${keycloak.server.url}")
    private lateinit var serverUrl: String

    @Value("\${keycloak.realm}")

    private lateinit var realm: String

    @Value("\${keycloak.client-id}")
    private lateinit var clientId: String

    @Value("\${keycloak.client-secret}")
    private lateinit var clientSecret: String

    private val keycloakWebClient by lazy {
        webClient.baseUrl(serverUrl).build()
    }

    /**
     * Authenticate user with Keycloak using Resource Owner Password Credentials flow
     */
    fun authenticateUser(username: String, password: String?): Mono<KeycloakTokenResponse> {
        logger.info("Attempting to authenticate user with Keycloak: {}", username)

        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("grant_type", "password")
        formData.add("client_id", clientId)
        formData.add("client_secret", clientSecret)
        formData.add("username", username)
        formData.add("password", password)
        formData.add("scope", "openid email profile");

        return keycloakWebClient
            .post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(KeycloakTokenResponse::class.java)
            .doOnSuccess { response ->
                logger.info("Successfully authenticated user with Keycloak: {}", username)
            }
            .doOnError { error ->
                when (error) {
                    is WebClientResponseException -> {
                        logger.warn(
                            "Keycloak authentication failed for user {}: {} - {}",
                            username, error.statusCode, error.responseBodyAsString
                        )
                    }

                    else -> {
                        logger.error("Keycloak authentication error for user {}: {}", username, error.message, error)
                    }
                }
            }
    }

    /**
     * Refresh access token using refresh token
     */
    fun refreshToken(refreshToken: String): Mono<KeycloakTokenResponse> {
        logger.debug("Refreshing token with Keycloak")

        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("grant_type", "refresh_token")
        formData.add("client_id", clientId)
        formData.add("client_secret", clientSecret)
        formData.add("refresh_token", refreshToken)

        return keycloakWebClient
            .post()
            .uri("/realms/{realm}/protocol/openid-connect/token", realm)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(KeycloakTokenResponse::class.java)
            .doOnSuccess {
                logger.debug("Successfully refreshed token with Keycloak")
            }
            .doOnError { error ->
                when (error) {
                    is WebClientResponseException -> {
                        logger.warn(
                            "Keycloak token refresh failed: {} - {}",
                            error.statusCode, error.responseBodyAsString
                        )
                    }

                    else -> {
                        logger.error("Keycloak token refresh error: {}", error.message, error)
                    }
                }
            }
    }

    /**
     * Get user info from Keycloak using access token
     */
    fun getUserInfo(accessToken: String): Mono<KeycloakUserInfo> {
        logger.debug("Fetching user info from Keycloak")

        return keycloakWebClient
            .get()
            .uri("/realms/{realm}/protocol/openid-connect/userinfo", realm)
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .retrieve()
            .bodyToMono(KeycloakUserInfo::class.java)
            .doOnSuccess { userInfo ->
                logger.debug("Successfully fetched user info from Keycloak: {}", userInfo.preferredUsername)
            }
            .doOnError { error ->
                when (error) {
                    is WebClientResponseException -> {
                        logger.warn(
                            "Failed to fetch user info from Keycloak: {} - {}",
                            error.statusCode, error.responseBodyAsString
                        )
                    }

                    else -> {
                        logger.error("Keycloak user info error: {}", error.message, error)
                    }
                }
            }
    }

    /**
     * Validate token by calling Keycloak's introspection endpoint
     */
    fun introspectToken(token: String): Mono<KeycloakTokenIntrospection> {
        logger.debug("Introspecting token with Keycloak")

        val formData: MultiValueMap<String, String> = LinkedMultiValueMap()
        formData.add("client_id", clientId)
        formData.add("client_secret", clientSecret)
        formData.add("token", token)

        return keycloakWebClient
            .post()
            .uri("/realms/{realm}/protocol/openid-connect/token/introspect", realm)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(KeycloakTokenIntrospection::class.java)
            .doOnSuccess { introspection ->
                logger.debug("Token introspection completed - active: {}", introspection.active)
            }
            .doOnError { error ->
                logger.error("Token introspection error: {}", error.message, error)
            }
    }
}