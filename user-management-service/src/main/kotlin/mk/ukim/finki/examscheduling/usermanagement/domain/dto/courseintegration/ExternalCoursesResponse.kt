package mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration

import com.fasterxml.jackson.annotation.JsonProperty

data class ExternalCoursesResponse(
    @JsonProperty("courses")
    val courses: List<ExternalCourseDTO>,

    @JsonProperty("statistics")
    val statistics: List<Map<String, Any>>,

    @JsonProperty("count")
    val count: Int
)
