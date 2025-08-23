package mk.ukim.finki.examscheduling.externalintegration.repository

import mk.ukim.finki.examscheduling.externalintegration.domain.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CourseRepository : JpaRepository<Course, UUID> {

    fun existsByExternalCourseId(externalCourseId: String): Boolean

    fun findByDepartment(department: String): List<Course>

    fun findByCourseNameContainingIgnoreCase(courseName: String): List<Course>

    fun findByCourseCodeContainingIgnoreCase(courseCode: String): List<Course>

    @Query("SELECT COUNT(c) FROM Course c WHERE c.department = :department")
    fun countByDepartment(@Param("department") department: String): Long

    @Query(
        value = """
            SELECT 
                department,
                COUNT(*) as total_courses,
                AVG(ects_credits) as avg_ects,
                MIN(ects_credits) as min_ects,
                MAX(ects_credits) as max_ects
            FROM courses
            WHERE department IS NOT NULL
            GROUP BY department
            ORDER BY total_courses DESC
        """,
        nativeQuery = true
    )
    fun getCourseStatisticsByDepartment(): List<Map<String, Any>>
}