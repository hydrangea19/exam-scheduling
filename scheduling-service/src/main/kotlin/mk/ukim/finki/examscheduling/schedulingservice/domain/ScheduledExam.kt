package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.persistence.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "scheduled_exams")
data class ScheduledExam(
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "scheduled_exam_id", nullable = false)
    val scheduledExamId: String,

    @Column(name = "course_id", nullable = false)
    val courseId: String,

    @Column(name = "course_name", nullable = false)
    val courseName: String,

    @Column(name = "exam_date", nullable = false)
    val examDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: java.time.LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: java.time.LocalTime,

    @Column(name = "room_id")
    val roomId: String?,

    @Column(name = "room_name")
    val roomName: String?,

    @Column(name = "room_capacity")
    val roomCapacity: Int?,

    @Column(name = "student_count", nullable = false)
    val studentCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "mandatory_status", nullable = false)
    val mandatoryStatus: MandatoryStatus,

    @ElementCollection
    @CollectionTable(name = "scheduled_exam_professors", joinColumns = [JoinColumn(name = "scheduled_exam_id")])
    @Column(name = "professor_id")
    val professorIds: MutableSet<String> = mutableSetOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    val updatedAt: Instant? = null,

    @Version
    val version: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_schedule_id", nullable = false)
    val examSessionSchedule: ExamSessionSchedule?
)
