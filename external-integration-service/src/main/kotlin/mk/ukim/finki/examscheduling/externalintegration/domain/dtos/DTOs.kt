package mk.ukim.finki.examscheduling.externalintegration.domain.dtos

import mk.ukim.finki.examscheduling.externalintegration.domain.courses.CourseEnrollment
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaSemesterDto
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom
import java.time.Duration
import java.time.Instant

data class CompleteExamCourseInfo(
    val course: ExamCourse,
    val enrollment: CourseEnrollment?,
    val availableProfessors: List<ExamProfessor>,
    val dataCompleteness: Double,
    val lastSynchronized: Instant
)

data class ExamSchedulingResources(
    val availableCourses: List<ExamCourse>,
    val availableProfessors: List<ExamProfessor>,
    val availableRooms: List<ExamRoom>,
    val currentSemester: RaspredelbaSemesterDto?,
    val lastSynchronized: Instant,
    val resourceCompleteness: Double
)

data class DataRefreshResult(
    val success: Boolean,
    val serviceResults: Map<String, Boolean>,
    val duration: Duration,
    val refreshedAt: Instant
)