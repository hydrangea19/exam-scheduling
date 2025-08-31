package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ExamSessionSchedule
import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduledExamInfo
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ExamSessionScheduleRepository : JpaRepository<ExamSessionSchedule, UUID> {

    fun findByAcademicYearAndExamSession(academicYear: String, examSession: String): List<ExamSessionSchedule>
    fun findByStatus(status: ScheduleStatus): List<ExamSessionSchedule>

    fun findByExamSessionPeriodId(examSessionPeriodId: String): ExamSessionSchedule

    @Query(
        """
        SELECT 
            COUNT(*) as total_schedules,
            COUNT(CASE WHEN status = 'DRAFT' THEN 1 END) as draft_schedules,
            COUNT(CASE WHEN status = 'GENERATED' THEN 1 END) as generated_schedules,
            COUNT(CASE WHEN status = 'PUBLISHED_FOR_REVIEW' THEN 1 END) as review_schedules,
            COUNT(CASE WHEN status = 'FINALIZED' THEN 1 END) as finalized_schedules,
            COUNT(CASE WHEN status = 'PUBLISHED' THEN 1 END) as published_schedules,
            COUNT(DISTINCT academic_year) as unique_years
        FROM exam_session_schedules
    """, nativeQuery = true
    )
    fun getScheduleStatistics(): Map<String, Any>

}