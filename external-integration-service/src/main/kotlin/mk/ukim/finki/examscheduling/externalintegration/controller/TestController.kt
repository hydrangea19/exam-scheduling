package mk.ukim.finki.examscheduling.externalintegration.controller

import mk.ukim.finki.examscheduling.externalintegration.domain.Course
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.CourseCreateRequest
import mk.ukim.finki.examscheduling.externalintegration.repository.CourseRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api/test")
class TestController @Autowired constructor(
    private val courseRepository: CourseRepository
) {

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "External Integration Service is running",
            "timestamp" to Instant.now(),
            "service" to "external-integration-service",
            "version" to "1.0.0-SNAPSHOT"
        )
    }

    @GetMapping("/courses")
    fun getAllCourses(): ResponseEntity<Map<String, Any?>> {
        return try {
            val courses = courseRepository.findAll()
            val statistics = courseRepository.getCourseStatisticsByDepartment()

            ResponseEntity.ok(
                mapOf(
                    "courses" to courses.map {
                        mapOf(
                            "id" to it.id,
                            "externalCourseId" to it.externalCourseId,
                            "courseCode" to it.courseCode,
                            "courseName" to it.courseName,
                            "displayName" to it.getDisplayName(),
                            "department" to it.department,
                            "semester" to it.semester,
                            "ectsCredits" to it.ectsCredits,
                            "createdAt" to it.createdAt
                        )
                    },
                    "statistics" to statistics,
                    "count" to courses.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch courses",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/courses/{id}")
    fun getCourseById(@PathVariable id: UUID): ResponseEntity<Map<String, Any?>> {
        return try {
            val course = courseRepository.findById(id)
            if (course.isPresent) {
                val c = course.get()
                ResponseEntity.ok(
                    mapOf(
                        "id" to c.id,
                        "externalCourseId" to c.externalCourseId,
                        "courseCode" to c.courseCode,
                        "courseName" to c.courseName,
                        "displayName" to c.getDisplayName(),
                        "department" to c.department,
                        "semester" to c.semester,
                        "semesterDisplay" to c.getSemesterDisplay(),
                        "ectsCredits" to c.ectsCredits,
                        "createdAt" to c.createdAt,
                        "updatedAt" to c.updatedAt
                    )
                )
            } else {
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch course",
                    "message" to e.message
                )
            )
        }
    }

    @PostMapping("/courses")
    fun createCourse(@RequestBody request: CourseCreateRequest): ResponseEntity<Map<String, Any?>> {
        return try {
            if (courseRepository.existsByExternalCourseId(request.externalCourseId)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    mapOf(
                        "error" to "Course with external ID ${request.externalCourseId} already exists"
                    )
                )
            }

            val newCourse = Course(
                externalCourseId = request.externalCourseId,
                courseCode = request.courseCode,
                courseName = request.courseName,
                ectsCredits = request.ectsCredits,
                semester = request.semester,
                department = request.department
            )

            val savedCourse = courseRepository.save(newCourse)

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "id" to savedCourse.id,
                    "externalCourseId" to savedCourse.externalCourseId,
                    "displayName" to savedCourse.getDisplayName(),
                    "message" to "Course created successfully"
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to create course",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/courses/search")
    fun searchCourses(@RequestParam query: String): ResponseEntity<Map<String, Any?>> {
        return try {
            val coursesByName = courseRepository.findByCourseNameContainingIgnoreCase(query)
            val coursesByCode = courseRepository.findByCourseCodeContainingIgnoreCase(query)
            val allResults = (coursesByName + coursesByCode).distinctBy { it.id }

            ResponseEntity.ok(
                mapOf(
                    "query" to query,
                    "results" to allResults.map {
                        mapOf(
                            "id" to it.id,
                            "externalCourseId" to it.externalCourseId,
                            "courseCode" to it.courseCode,
                            "courseName" to it.courseName,
                            "displayName" to it.getDisplayName(),
                            "department" to it.department
                        )
                    },
                    "count" to allResults.size
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Search failed",
                    "message" to e.message
                )
            )
        }
    }

    @GetMapping("/courses/department/{department}")
    fun getCoursesByDepartment(@PathVariable department: String): ResponseEntity<Map<String, Any?>> {
        return try {
            val courses = courseRepository.findByDepartment(department)
            val courseCount = courseRepository.countByDepartment(department)

            ResponseEntity.ok(
                mapOf(
                    "department" to department,
                    "courses" to courses.map {
                        mapOf(
                            "id" to it.id,
                            "courseCode" to it.courseCode,
                            "courseName" to it.courseName,
                            "semester" to it.semester,
                            "ectsCredits" to it.ectsCredits
                        )
                    },
                    "count" to courseCount
                )
            )
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "error" to "Failed to fetch courses by department",
                    "message" to e.message
                )
            )
        }
    }
}