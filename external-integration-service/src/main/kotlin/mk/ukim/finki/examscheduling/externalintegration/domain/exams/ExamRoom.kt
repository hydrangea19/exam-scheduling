package mk.ukim.finki.examscheduling.externalintegration.domain.exams

import mk.ukim.finki.examscheduling.externalintegration.domain.enums.ExamRoomType
import java.time.Instant

data class ExamRoom(
    val roomId: String,
    val roomName: String,
    val capacity: Int,
    val building: String,
    val roomType: ExamRoomType,
    val hasSpecialEquipment: Boolean = false,
    val equipmentList: List<String> = emptyList(),
    val availabilityPeriod: RoomAvailabilityPeriod,
    val lastUpdated: Instant = Instant.now()
) {
    fun canAccommodateStudents(requiredCapacity: Int): Boolean = capacity >= requiredCapacity

    fun isSuitableForCourse(course: ExamCourse): Boolean {
        return when {
            course.requiresSpecialRoom() && !hasSpecialEquipment -> false
            course.courseName.contains("Computer", ignoreCase = true) &&
                    roomType != ExamRoomType.COMPUTER_LAB -> false

            else -> true
        }
    }
}