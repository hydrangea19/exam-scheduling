package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ProfessorComment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ProfessorCommentRepository : JpaRepository<ProfessorComment, UUID> {

    fun findByCommentId(commentId: String): ProfessorComment?
    fun findByProfessorIdAndExamSessionScheduleId(
        professorId: String,
        scheduleId: java.util.UUID
    ): List<ProfessorComment>

    fun findByExamSessionScheduleId(examSessionScheduleId: UUID): List<ProfessorComment>

    @Query("SELECT pc FROM ProfessorComment pc WHERE pc.status = 'SUBMITTED' ORDER BY pc.submittedAt ASC")
    fun findPendingComments(): List<ProfessorComment>

    @Query(
        """
        SELECT 
            COUNT(*) as total_comments,
            COUNT(CASE WHEN status = 'SUBMITTED' THEN 1 END) as submitted_comments,
            COUNT(CASE WHEN status = 'UNDER_REVIEW' THEN 1 END) as under_review_comments,
            COUNT(CASE WHEN status = 'APPROVED' THEN 1 END) as approved_comments,
            COUNT(CASE WHEN status = 'REJECTED' THEN 1 END) as rejected_comments,
            COUNT(CASE WHEN status = 'IMPLEMENTED' THEN 1 END) as implemented_comments,
            COUNT(DISTINCT professor_id) as unique_professors
        FROM professor_comments
    """, nativeQuery = true
    )
    fun getCommentStatistics(): Map<String, Any>
}