package mk.ukim.finki.examscheduling.externalintegration.domain.dtos

data class CourseCreateRequest(
    val externalCourseId: String,
    val courseCode: String,
    val courseName: String,
    val ectsCredits: Int? = null,
    val semester: Int? = null,
    val department: String? = null
)
