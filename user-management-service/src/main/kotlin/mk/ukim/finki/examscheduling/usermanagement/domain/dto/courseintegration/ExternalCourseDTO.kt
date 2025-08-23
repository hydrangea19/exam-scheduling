package mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.*

data class ExternalCourseDTO(
    @JsonProperty("id")
    val id: UUID,

    @JsonProperty("externalCourseId")
    val externalCourseId: String,

    @JsonProperty("courseCode")
    val courseCode: String,

    @JsonProperty("courseName")
    val courseName: String,

    @JsonProperty("displayName")
    val displayName: String,

    @JsonProperty("department")
    val department: String?,

    @JsonProperty("semester")
    val semester: Int?,

    @JsonProperty("ectsCredits")
    val ectsCredits: Int?,

    @JsonProperty("createdAt")
    val createdAt: Instant
)
