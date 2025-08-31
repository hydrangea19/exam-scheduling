package mk.ukim.finki.examscheduling.schedulingservice.repository

import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduleVersionEntity
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleVersionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ScheduleVersionRepository : JpaRepository<ScheduleVersionEntity, UUID> {
    fun findByScheduleId(scheduleId: UUID): List<ScheduleVersionEntity>
    fun findByScheduleIdOrderByVersionNumberDesc(scheduleId: UUID): List<ScheduleVersionEntity>
    fun findByScheduleIdAndVersionNumber(scheduleId: UUID, versionNumber: Int): ScheduleVersionEntity?
    fun findByVersionType(versionType: ScheduleVersionType): List<ScheduleVersionEntity>
}