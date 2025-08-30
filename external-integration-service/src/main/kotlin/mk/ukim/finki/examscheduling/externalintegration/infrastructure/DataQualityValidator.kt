package mk.ukim.finki.examscheduling.externalintegration.infrastructure

import mk.ukim.finki.examscheduling.externalintegration.domain.courses.CourseEnrollment
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.ValidationResult
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DataQualityValidator {
    private val logger = LoggerFactory.getLogger(DataQualityValidator::class.java)

    fun validateCourseData(course: ExamCourse): ValidationResult {
        val errors = mutableListOf<String>()

        if (course.courseId.isBlank()) {
            errors.add("Course ID cannot be blank")
        }

        if (course.courseName.isBlank()) {
            errors.add("Course name cannot be blank")
        }

        if (course.credits <= 0) {
            errors.add("Course credits must be positive")
        }

        if (course.credits > 15) {
            errors.add("Course credits seem unusually high: ${course.credits}")
        }

        if (course.semester < 1 || course.semester > 8) {
            errors.add("Semester should be between 1 and 8, got: ${course.semester}")
        }

        if (course.estimatedDurationMinutes < 60 || course.estimatedDurationMinutes > 240) {
            errors.add("Exam duration should be between 60 and 240 minutes, got: ${course.estimatedDurationMinutes}")
        }

        if (course.professors.isEmpty()) {
            errors.add("Course must have at least one professor assigned")
        }

        course.professors.forEach { professor ->
            if (professor.professorId.isBlank()) {
                errors.add("Professor ID cannot be blank")
            }
            if (professor.fullName.isBlank()) {
                errors.add("Professor name cannot be blank")
            }
            if (!professor.email.contains("@")) {
                errors.add("Professor email is invalid: ${professor.email}")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateProfessorData(professor: ExamProfessor): ValidationResult {
        val errors = mutableListOf<String>()

        if (professor.professorId.isBlank()) {
            errors.add("Professor ID cannot be blank")
        }

        if (professor.firstName.isBlank() || professor.lastName.isBlank()) {
            errors.add("Professor first name and last name cannot be blank")
        }

        if (!professor.email.contains("@")) {
            errors.add("Professor email is invalid: ${professor.email}")
        }

        if (professor.department.isBlank()) {
            errors.add("Professor department cannot be blank")
        }

        if (professor.maxExamsPerDay < 1 || professor.maxExamsPerDay > 5) {
            errors.add("Max exams per day should be between 1 and 5, got: ${professor.maxExamsPerDay}")
        }

        professor.availabilityPeriod?.let { period ->
            if (period.availableFrom.isAfter(period.availableTo)) {
                errors.add("Professor availability 'from' date must be before 'to' date")
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateRoomData(room: ExamRoom): ValidationResult {
        val errors = mutableListOf<String>()

        if (room.roomId.isBlank()) {
            errors.add("Room ID cannot be blank")
        }

        if (room.roomName.isBlank()) {
            errors.add("Room name cannot be blank")
        }

        if (room.capacity <= 0) {
            errors.add("Room capacity must be positive")
        }

        if (room.capacity > 500) {
            errors.add("Room capacity seems unusually high: ${room.capacity}")
        }

        if (room.building.isBlank()) {
            errors.add("Room building cannot be blank")
        }

        val availabilityPeriod = room.availabilityPeriod
        if (availabilityPeriod.availableFrom.isAfter(availabilityPeriod.availableTo)) {
            errors.add("Room availability 'from' date must be before 'to' date")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun validateEnrollmentData(enrollment: CourseEnrollment): ValidationResult {
        val errors = mutableListOf<String>()

        if (enrollment.courseId.isBlank()) {
            errors.add("Course ID cannot be blank")
        }

        if (enrollment.semesterCode.isBlank()) {
            errors.add("Semester code cannot be blank")
        }

        if (enrollment.totalEnrolledStudents < 0) {
            errors.add("Total enrolled students cannot be negative")
        }

        if (enrollment.activeEnrollments < 0) {
            errors.add("Active enrollments cannot be negative")
        }

        if (enrollment.estimatedExamAttendance < 0) {
            errors.add("Estimated exam attendance cannot be negative")
        }

        if (enrollment.activeEnrollments > enrollment.totalEnrolledStudents) {
            errors.add("Active enrollments cannot exceed total enrolled students")
        }

        if (enrollment.estimatedExamAttendance > enrollment.totalEnrolledStudents) {
            errors.add("Estimated exam attendance cannot exceed total enrolled students")
        }

        if (enrollment.isEnrollmentStale()) {
            errors.add("Enrollment data is stale (older than 24 hours)")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}