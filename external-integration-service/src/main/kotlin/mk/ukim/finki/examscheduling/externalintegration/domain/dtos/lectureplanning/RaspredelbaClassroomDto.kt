package mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning

data class RaspredelbaClassroomDto(
    val id: String,
    val name: String,
    val capacity: Int,
    val building: String,
    val maintenanceStart: String? = null,
    val maintenanceEnd: String? = null,
    val availableFrom: String,
    val availableTo: String,
    val lastAuditDate: String
)