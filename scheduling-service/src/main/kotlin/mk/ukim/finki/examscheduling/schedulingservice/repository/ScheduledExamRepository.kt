package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduledExam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.*

@Repository
interface ScheduledExamRepository : JpaRepository<ScheduledExam, UUID> {

    fun findByScheduledExamId(scheduledExamId: String): ScheduledExam?

    fun findByExamDate(examDate: LocalDate): List<ScheduledExam>

    fun findByExamSessionScheduleId(examSessionScheduleId: UUID): List<ScheduledExam>

    @Query(
        """
        SELECT se 
        FROM ScheduledExam se 
        JOIN se.professorIds p 
        WHERE p = :professorId
    """
    )
    fun findByProfessorId(professorId: String): List<ScheduledExam>

    @Query(
        """
        SELECT 
            exam_date,
            COUNT(*) as exams_count,
            MIN(start_time) as first_exam_time,
            MAX(end_time) as last_exam_time,
            COUNT(DISTINCT room_id) as rooms_used
        FROM scheduled_exams
        GROUP BY exam_date
        ORDER BY exam_date
    """, nativeQuery = true
    )
    fun getDailyExamStatistics(): List<Map<String, Any>>
}