package mk.ukim.finki.examscheduling.sharedsecurity.dto.keycloak

data class KeycloakTokenIntrospection(
    val active: Boolean = false,
    val sub: String? = null,
    val username: String? = null,
    val email: String? = null,
    val exp: Long? = null,
    val iat: Long? = null,
    val iss: String? = null,
    val aud: List<String> = emptyList(),
    val typ: String? = null,
    val azp: String? = null
)
