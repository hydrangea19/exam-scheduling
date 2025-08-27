package mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak

data class KeycloakTokenResponse(
    @com.fasterxml.jackson.annotation.JsonProperty("access_token")
    val accessToken: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("refresh_token")
    val refreshToken: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("token_type")
    val tokenType: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
    val expiresIn: Long? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("refresh_expires_in")
    val refreshExpiresIn: Long? = null,

    val scope: String? = null
)