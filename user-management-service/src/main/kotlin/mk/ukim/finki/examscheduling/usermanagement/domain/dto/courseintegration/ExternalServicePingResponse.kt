package mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ExternalServicePingResponse(
    @JsonProperty("message")
    val message: String,

    @JsonProperty("timestamp")
    val timestamp: Instant,

    @JsonProperty("service")
    val service: String,

    @JsonProperty("version")
    val version: String,

    @JsonProperty("database")
    val database: String?
)
