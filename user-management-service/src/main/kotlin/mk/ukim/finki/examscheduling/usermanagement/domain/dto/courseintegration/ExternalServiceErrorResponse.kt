package mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ExternalServiceErrorResponse(
    @JsonProperty("error")
    val error: String,

    @JsonProperty("message")
    val message: String?,

    @JsonProperty("timestamp")
    val timestamp: Instant = Instant.now(),

    @JsonProperty("path")
    val path: String? = null,

    @JsonProperty("status")
    val status: Int? = null
)
