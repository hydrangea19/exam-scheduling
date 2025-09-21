package mk.ukim.finki.examscheduling.schedulingservice.domain

import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity

data class PythonSchedulingRequest(
    val examPeriod: ExamPeriod,
    val courses: List<CourseSchedulingInfo>,
    val availableRooms: List<RoomInfo>,
    val professorPreferences: List<ProfessorPreferenceInfo>,
    val institutionalConstraints: InstitutionalConstraints
)

data class PythonSchedulingResponse(
    val success: Boolean,
    val errorMessage: String? = null,
    val scheduledExams: List<ScheduledExamInfo>,
    val metrics: PythonSchedulingMetrics,
    val qualityScore: Double,
    val violations: List<PythonConstraintViolation>,
    val processingTimeMs: Long = 0L,
    val algorithmUsed: String? = null
) {
    fun toSchedulingResult(): SchedulingResult {
        return SchedulingResult(
            scheduledExams = this.scheduledExams,
            metrics = this.metrics.toSchedulingMetrics(),
            qualityScore = this.qualityScore,
            violations = this.violations.map { it.toConstraintViolation() }
        )
    }
}

data class PythonConstraintViolation(
    val violationType: String,
    val severity: String,
    val description: String,
    val affectedExamIds: List<String>,
    val affectedStudents: Int,
    val suggestedResolution: String? = null
) {
    fun toConstraintViolation(): ConstraintViolation {
        return ConstraintViolation(
            violationType = this.violationType,
            severity = ViolationSeverity.valueOf(this.severity),
            description = this.description,
            affectedExams = this.affectedExamIds,
            affectedStudents = this.affectedStudents,
            suggestedResolution = this.suggestedResolution
        )
    }
}

data class PythonSchedulingMetrics(
    val totalCoursesScheduled: Int,
    val totalProfessorPreferencesConsidered: Int,
    val preferencesSatisfied: Int,
    val preferenceSatisfactionRate: Double,
    val totalConflicts: Int,
    val resolvedConflicts: Int,
    val roomUtilizationRate: Double,
    val averageStudentExamsPerDay: Double,
    val processingTimeMs: Long
) {
    fun toSchedulingMetrics(): SchedulingMetrics {
        return SchedulingMetrics(
            totalCoursesScheduled = this.totalCoursesScheduled,
            totalProfessorPreferencesConsidered = this.totalProfessorPreferencesConsidered,
            preferencesSatisfied = this.preferencesSatisfied,
            preferenceSatisfactionRate = this.preferenceSatisfactionRate,
            totalConflicts = this.totalConflicts,
            resolvedConflicts = this.resolvedConflicts,
            roomUtilizationRate = this.roomUtilizationRate,
            averageStudentExamsPerDay = this.averageStudentExamsPerDay,
            processingTimeMs = this.processingTimeMs
        )
    }
}

data class PythonServiceHealthResponse(
    val status: String,
    val timestamp: String,
    val version: String? = null,
    val uptime: Long? = null
)