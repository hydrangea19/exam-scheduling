package mk.ukim.finki.examscheduling.externalintegration.domain

import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse
import org.slf4j.LoggerFactory
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class CourseDataCache(
    private val cacheManager: CacheManager
) {
    private val logger = LoggerFactory.getLogger(CourseDataCache::class.java)
    private val courseCache = cacheManager.getCache("courses")
    private val allCoursesCache = cacheManager.getCache("all-courses")
    private var lastFullCacheUpdate: Instant = Instant.MIN

    fun getCourse(courseId: String): ExamCourse? {
        return courseCache?.get(courseId, ExamCourse::class.java)
    }

    fun cacheCourse(course: ExamCourse) {
        courseCache?.put(course.courseId, course)
        logger.debug("Cached course: {}", course.courseId)
    }

    fun getAllCourses(): List<ExamCourse> {
        return allCoursesCache?.get("all", List::class.java) as? List<ExamCourse> ?: emptyList()
    }

    fun cacheAllCourses(courses: List<ExamCourse>) {
        allCoursesCache?.put("all", courses)
        courses.forEach { cacheCourse(it) }
        lastFullCacheUpdate = Instant.now()
        logger.info("Cached {} courses", courses.size)
    }

    fun clearCache() {
        courseCache?.clear()
        allCoursesCache?.clear()
        lastFullCacheUpdate = Instant.MIN
        logger.info("Course cache cleared")
    }

    fun isStale(): Boolean {
        return Duration.between(lastFullCacheUpdate, Instant.now()).toHours() > 6
    }
}