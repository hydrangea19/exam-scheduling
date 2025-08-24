package mk.ukim.finki.examscheduling.sharedsecurity.jwt

import io.jsonwebtoken.*
import io.jsonwebtoken.jackson.io.JacksonDeserializer
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class JwtTokenProvider {

    private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    @Value("\${jwt.secret:MySecretKeyForExamSchedulingSystemThatIsLongEnoughForHMAC512}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration:86400}")
    private var jwtExpiration: Int = 86400 // 24h in seconds

    private fun getSigningKey(): Key = Keys.hmacShaKeyFor(jwtSecret.toByteArray())

    fun generateToken(userId: String, email: String, role: String, fullName: String? = null): String {
        val now = Instant.now()
        val expiry = now.plus(jwtExpiration.toLong(), ChronoUnit.SECONDS)

        return Jwts.builder()
            .setSubject(userId)
            .claim("email", email)
            .claim("role", role)
            .claim("fullName", fullName ?: email)
            .claim("tokenType", "access")
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .serializeToJsonWith(JacksonSerializer())
            .compact()
    }

    fun generateRefreshToken(userId: String): String {
        val now = Instant.now()
        val expiry = now.plus((jwtExpiration * 7).toLong(), ChronoUnit.SECONDS) // 7 days

        return Jwts.builder()
            .setSubject(userId)
            .claim("tokenType", "refresh")
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .serializeToJsonWith(JacksonSerializer())
            .compact()
    }

    private fun parseToken(token: String): Jws<Claims>? {
        return try {
            Jwts.parserBuilder()
                .deserializeJsonWith(JacksonDeserializer(mapOf()))
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
        } catch (e: Exception) {
            logger.warn("Failed to parse JWT: {}", e.message)
            null
        }
    }

    fun getUserIdFromToken(token: String): String? = parseToken(token)?.body?.subject

    fun getEmailFromToken(token: String): String? = parseToken(token)?.body?.get("email", String::class.java)

    fun getRoleFromToken(token: String): String? = parseToken(token)?.body?.get("role", String::class.java)

    fun getFullNameFromToken(token: String): String? = parseToken(token)?.body?.get("fullName", String::class.java)

    fun validateToken(token: String): Boolean {
        return try {
            parseToken(token) != null
        } catch (e: SecurityException) {
            logger.error("Invalid JWT signature: {}", e.message)
            false
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token: {}", e.message)
            false
        } catch (e: ExpiredJwtException) {
            logger.error("JWT token expired: {}", e.message)
            false
        } catch (e: UnsupportedJwtException) {
            logger.error("JWT token unsupported: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            logger.error("JWT claims empty: {}", e.message)
            false
        }
    }

    fun isTokenExpired(token: String): Boolean {
        val expiry = parseToken(token)?.body?.expiration ?: return true
        return expiry.before(Date())
    }

    fun getExpirationDateFromToken(token: String): Date? = parseToken(token)?.body?.expiration
}