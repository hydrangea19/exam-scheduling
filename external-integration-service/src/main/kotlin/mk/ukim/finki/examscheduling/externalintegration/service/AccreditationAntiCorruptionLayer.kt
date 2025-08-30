package mk.ukim.finki.examscheduling.externalintegration.service

import mk.ukim.finki.examscheduling.externalintegration.domain.exams.ExamCourse

interface AccreditationAntiCorruptionLayer {
    fun getCourseInformation(courseId: String): ExamCourse?
    fun getAllCourses(): List<ExamCourse>
    fun getCoursesByStudyProgram(studyProgramId: String): List<ExamCourse>
    fun getCoursesForSemester(semester: Int): List<ExamCourse>
    fun refreshCourseCache()
}