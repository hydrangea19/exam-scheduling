package mk.ukim.finki.examscheduling.externalintegration.service

import mk.ukim.finki.examscheduling.externalintegration.domain.courses.CourseEnrollment
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaSemesterDto
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamProfessor
import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamRoom

interface RaspredelbaAntiCorruptionLayer {
    fun getProfessorAvailability(professorId: String): ExamProfessor?
    fun getAllAvailableProfessors(): List<ExamProfessor>
    fun getProfessorsForSubject(subjectCode: String): List<ExamProfessor>
    fun getClassroomAvailability(classroomId: String): ExamRoom?
    fun getAllAvailableClassrooms(): List<ExamRoom>
    fun getEnrollmentData(courseId: String, semesterCode: String): CourseEnrollment?
    fun getCurrentSemesterInfo(): RaspredelbaSemesterDto?
    fun refreshAvailabilityCache()
}