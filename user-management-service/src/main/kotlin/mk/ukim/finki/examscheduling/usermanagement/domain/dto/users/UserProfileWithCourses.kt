package mk.ukim.finki.examscheduling.usermanagement.domain.dto.users

import mk.ukim.finki.examscheduling.usermanagement.domain.dto.courseintegration.ExternalCourseDTO
import java.util.*

data class UserProfileWithCourses(
    val userId: UUID,
    val email: String,
    val fullName: String,
    val preferredCourses: List<ExternalCourseDTO>,
    val departmentPreferences: List<String>,
    val semesterPreferences: List<Int>
)
