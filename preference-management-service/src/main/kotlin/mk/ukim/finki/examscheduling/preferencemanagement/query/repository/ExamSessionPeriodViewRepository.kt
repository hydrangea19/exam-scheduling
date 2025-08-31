package mk.ukim.finki.examscheduling.preferencemanagement.query.repository

import mk.ukim.finki.examscheduling.preferencemanagement.query.ExamSessionPeriodView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExamSessionPeriodViewRepository : JpaRepository<ExamSessionPeriodView, String> {
    fun findByIsWindowOpenTrue(): List<ExamSessionPeriodView>
    fun findByAcademicYearOrderByCreatedAtDesc(academicYear: String): List<ExamSessionPeriodView>
}