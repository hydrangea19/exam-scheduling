package mk.ukim.finki.examscheduling.preferencemanagement.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import mk.ukim.finki.examscheduling.preferencemanagement.domain.enums.PreferenceStatus
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*


@Entity
@Table(
    name = "professor_preferences",
    indexes = [
        Index(name = "idx_professor_preferences_professor_id", columnList = "professor_id"),
        Index(name = "idx_professor_preferences_academic_year", columnList = "academic_year"),
        Index(name = "idx_professor_preferences_session", columnList = "exam_session"),
        Index(name = "idx_professor_preferences_status", columnList = "status")
    ]
)
data class ProfessorPreference(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "professor_id", nullable = false)
    val professorId: UUID,

    @Column(name = "academic_year", nullable = false, length = 20)
    @NotBlank(message = "Academic year is required")
    @Size(max = 20, message = "Academic year cannot exceed 20 characters")
    val academicYear: String,

    @Column(name = "exam_session", nullable = false, length = 50)
    @NotBlank(message = "Exam session is required")
    @Size(max = 50, message = "Exam session cannot exceed 50 characters")
    val examSession: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: PreferenceStatus = PreferenceStatus.DRAFT,

    @Column(name = "submitted_at")
    val submittedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0
) {
    constructor() : this(
        id = UUID.randomUUID(),
        professorId = UUID.randomUUID(),
        academicYear = "",
        examSession = ""
    )

    fun isSubmitted(): Boolean = status == PreferenceStatus.SUBMITTED

    fun isDraft(): Boolean = status == PreferenceStatus.DRAFT

    fun getDisplayName(): String = "$academicYear $examSession Preferences"

    fun submit(): ProfessorPreference {
        return copy(
            status = PreferenceStatus.SUBMITTED,
            submittedAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    fun update(
        academicYear: String = this.academicYear,
        examSession: String = this.examSession,
        status: PreferenceStatus = this.status
    ): ProfessorPreference {
        return copy(
            academicYear = academicYear,
            examSession = examSession,
            status = status,
            updatedAt = Instant.now()
        )
    }

    override fun toString(): String {
        return "ProfessorPreference(id=$id, professorId=$professorId, academicYear='$academicYear', examSession='$examSession', status=$status)"
    }
}
