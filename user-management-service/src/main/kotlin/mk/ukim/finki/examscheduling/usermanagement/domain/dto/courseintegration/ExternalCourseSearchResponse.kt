package mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration

import com.fasterxml.jackson.annotation.JsonProperty

data class ExternalCourseSearchResponse(
    @JsonProperty("query")
    val query: String,

    @JsonProperty("results")
    val results: List<ExternalCourseDTO>,

    @JsonProperty("count")
    val count: Int
)
