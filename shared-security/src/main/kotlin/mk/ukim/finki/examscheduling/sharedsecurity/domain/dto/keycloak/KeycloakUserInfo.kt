package mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak

data class KeycloakUserInfo(
    val sub: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("preferred_username")
    val preferredUsername: String? = null,

    val email: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("email_verified")
    val emailVerified: Boolean = false,

    val name: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("given_name")
    val givenName: String? = null,

    @com.fasterxml.jackson.annotation.JsonProperty("family_name")
    val familyName: String? = null,

    val roles: List<String> = emptyList()
)