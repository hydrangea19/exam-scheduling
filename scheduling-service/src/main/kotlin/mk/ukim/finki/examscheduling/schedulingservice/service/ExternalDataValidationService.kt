package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.CourseAccreditationInfo
import mk.ukim.finki.examscheduling.schedulingservice.domain.CourseEnrollmentInfo
import mk.ukim.finki.examscheduling.schedulingservice.domain.ProfessorPreferenceInfo
import mk.ukim.finki.examscheduling.schedulingservice.domain.RoomInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExternalDataValidationService {

    private val logger = LoggerFactory.getLogger(ExternalDataValidationService::class.java)


    fun validateCourseData(courseInfo: CourseAccreditationInfo): Boolean {
        try {
            if (courseInfo.courseId.isBlank() || courseInfo.courseName.isBlank()) {
                logger.warn("Course missing required fields: {}", courseInfo.courseId)
                return false
            }

            if (!isValidCourseIdFormat(courseInfo.courseId)) {
                logger.warn("Invalid course ID format: {}", courseInfo.courseId)
                return false
            }

            if (courseInfo.credits < 1 || courseInfo.credits > 12) {
                logger.warn("Invalid credits for course {}: {}", courseInfo.courseId, courseInfo.credits)
                return false
            }

            if (courseInfo.professorIds.isEmpty()) {
                logger.warn("No professors assigned to course: {}", courseInfo.courseId)
            }

            if (courseInfo.courseName.length < 3 || courseInfo.courseName.length > 200) {
                logger.warn("Invalid course name length for {}: {}", courseInfo.courseId, courseInfo.courseName.length)
                return false
            }

            return true

        } catch (e: Exception) {
            logger.error("Error validating course data for {}: {}", courseInfo.courseId, e.message)
            return false
        }
    }

    fun validateEnrollmentData(enrollmentInfo: CourseEnrollmentInfo): Boolean {
        try {
            // Required field validation
            if (enrollmentInfo.courseId.isBlank()) {
                logger.warn("Enrollment missing course ID")
                return false
            }

            if (enrollmentInfo.studentCount < 0 || enrollmentInfo.studentCount > 1000) {
                logger.warn("Invalid student count for course {}: {}",
                    enrollmentInfo.courseId, enrollmentInfo.studentCount)
                return false
            }

            if (enrollmentInfo.studentCount == 0) {
                logger.warn("Zero enrollment for course: {}", enrollmentInfo.courseId)
            }

            return true

        } catch (e: Exception) {
            logger.error("Error validating enrollment data for {}: {}", enrollmentInfo.courseId, e.message)
            return false
        }
    }


    fun validatePreferenceData(preference: ProfessorPreferenceInfo): Boolean {
        try {
            // Required field validation
            if (preference.professorId.isBlank() || preference.courseId.isBlank()) {
                logger.warn("Preference missing required fields: professorId={}, courseId={}",
                    preference.professorId, preference.courseId)
                return false
            }

            val invalidDates = preference.preferredDates.filter { date ->
                date.isBefore(java.time.LocalDate.now().minusDays(30)) ||
                        date.isAfter(java.time.LocalDate.now().plusMonths(6))
            }

            if (invalidDates.isNotEmpty()) {
                logger.warn("Preference has invalid dates for {}: {}", preference.courseId, invalidDates)
                return false
            }

            val invalidTimeSlots = preference.preferredTimeSlots.filter { slot ->
                slot.startTime.isAfter(slot.endTime) ||
                        slot.startTime.isBefore(java.time.LocalTime.of(6, 0)) ||
                        slot.endTime.isAfter(java.time.LocalTime.of(23, 0))
            }

            if (invalidTimeSlots.isNotEmpty()) {
                logger.warn("Preference has invalid time slots for {}: {}", preference.courseId, invalidTimeSlots.size)
                return false
            }

            if (preference.priority < 1 || preference.priority > 5) {
                logger.warn("Invalid priority for preference {}: {}", preference.preferenceId, preference.priority)
                return false
            }

            return true

        } catch (e: Exception) {
            logger.error("Error validating preference data for {}: {}", preference.preferenceId, e.message)
            return false
        }
    }


    fun validateRoomData(roomInfo: RoomInfo): Boolean {
        try {
            // Required field validation
            if (roomInfo.roomId.isBlank() || roomInfo.roomName.isBlank()) {
                logger.warn("Room missing required fields: {}", roomInfo.roomId)
                return false
            }

            if (roomInfo.capacity < 5 || roomInfo.capacity > 500) {
                logger.warn("Invalid room capacity for {}: {}", roomInfo.roomId, roomInfo.capacity)
                return false
            }

            val validEquipment = setOf(
                "projector", "whiteboard", "blackboard", "sound-system",
                "computers", "recording", "microphone", "screen"
            )

            val invalidEquipment = roomInfo.equipment.minus(validEquipment)
            if (invalidEquipment.isNotEmpty()) {
                logger.warn("Room {} has unrecognized equipment: {}", roomInfo.roomId, invalidEquipment)
            }

            return true

        } catch (e: Exception) {
            logger.error("Error validating room data for {}: {}", roomInfo.roomId, e.message)
            return false
        }
    }


    private fun isValidCourseIdFormat(courseId: String): Boolean {
        val courseIdPattern = Regex("^[A-Z][0-9]{2}[A-Z][0-9][A-Z][0-9]{3}$")
        return courseId.matches(courseIdPattern) || courseId.length >= 3
    }
}