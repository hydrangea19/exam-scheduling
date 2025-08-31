package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduleConflictEntity
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictSeverity
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ScheduleConflictRepository : JpaRepository<ScheduleConflictEntity, UUID> {
    fun findByScheduleId(scheduleId: UUID): List<ScheduleConflictEntity>
    fun findByScheduleIdAndStatus(scheduleId: UUID, status: ConflictStatus): List<ScheduleConflictEntity>
    fun findByConflictId(conflictId: String): ScheduleConflictEntity?
    fun findBySeverityAndStatusOrderByDetectedAtDesc(severity: ConflictSeverity, status: ConflictStatus): List<ScheduleConflictEntity>
}