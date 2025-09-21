package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.RoomInfo
import mk.ukim.finki.examscheduling.schedulingservice.domain.ScheduledExamInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ResourceManagementService {

    private val logger = LoggerFactory.getLogger(ResourceManagementService::class.java)

    fun validateRoomAllocations(
        scheduledExams: List<ScheduledExamInfo>,
        availableRooms: List<RoomInfo>
    ): List<String> {
        val issues = mutableListOf<String>()

        scheduledExams.forEach { exam ->
            if (exam.roomCapacity != null && exam.studentCount > exam.roomCapacity) {
                issues.add("Room ${exam.roomName} capacity (${exam.roomCapacity}) exceeded by ${exam.studentCount - exam.roomCapacity} students for exam ${exam.courseId}")
            }

            val room = availableRooms.find { it.roomId == exam.roomId }
            if (room == null) {
                issues.add("Room ${exam.roomId} not found in available rooms for exam ${exam.courseId}")
            }
        }

        return issues
    }

    fun calculateBasicUtilization(scheduledExams: List<ScheduledExamInfo>): Double {
        val utilizations = scheduledExams.mapNotNull { exam ->
            exam.roomCapacity?.let { capacity ->
                if (capacity > 0) exam.studentCount.toDouble() / capacity else null
            }
        }
        return if (utilizations.isNotEmpty()) utilizations.average() else 0.0
    }
}