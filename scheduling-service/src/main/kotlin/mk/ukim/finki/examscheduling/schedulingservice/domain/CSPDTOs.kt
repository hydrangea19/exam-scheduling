package mk.ukim.finki.examscheduling.schedulingservice.domain

import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class CSPState(
    val problem: SchedulingProblem,
    val variables: MutableMap<String, Variable>,
    val domains: MutableMap<String, MutableSet<TimeSlot>>,
    val assignments: MutableMap<String, TimeSlot>,
    val constraintViolations: MutableList<ConstraintViolation>
)

data class CSPSolution(
    val assignments: MutableMap<String, TimeSlot>
)

data class Variable(
    val courseId: String,
    val courseName: String,
    val studentCount: Int,
    val professorIds: Set<String>,
    val mandatoryStatus: MandatoryStatus,
    val estimatedDuration: Int
)

data class TimeSlot(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val roomId: String,
    val roomName: String,
    val roomCapacity: Int,
    val dayOfWeek: Int
)

enum class SolvingStrategy {
    BACKTRACKING_WITH_FORWARD_CHECKING,
    SIMULATED_ANNEALING,
    HYBRID_APPROACH,
    GREEDY_WITH_BACKTRACKING
}

data class ConstraintValidationResult(
    val isValid: Boolean,
    val hardViolations: List<ConstraintViolation>,
    val softViolations: List<ConstraintViolation>,
    val qualityScore: Double,
    val totalConstraintsChecked: Int
)

data class SchedulingProblem(
    val examPeriod: ExamPeriod,
    val courses: List<CourseSchedulingInfo>,
    val availableRooms: List<RoomInfo>,
    val professorPreferences: List<ProfessorPreferenceInfo>,
    val institutionalConstraints: InstitutionalConstraints,
    val constraints: List<SchedulingConstraint>,
    val solvingStrategy: SolvingStrategy = SolvingStrategy.HYBRID_APPROACH
)

data class ExamPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val name: String
)

data class CourseSchedulingInfo(
    val courseId: String,
    val courseName: String,
    val studentCount: Int,
    val professorIds: Set<String>,
    val mandatoryStatus: MandatoryStatus,
    val estimatedDuration: Int,
    val requiredEquipment: Set<String> = emptySet(),
    val accessibilityRequired: Boolean = false,
    val specialRequirements: String? = null
)

data class InstitutionalConstraints(
    val workingHours: WorkingHours,
    val minimumExamDuration: Int = 120, // minutes
    val minimumGapMinutes: Int = 30,
    val maxExamsPerDay: Int = 8,
    val maxExamsPerRoom: Int = 6,
    val allowWeekendExams: Boolean = false
)

data class WorkingHours(
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class SchedulingConstraint(
    val id: String,
    val type: ConstraintType,
    val priority: ConstraintPriority,
    val description: String,
    val parameters: Map<String, Any> = emptyMap()
)

enum class ConstraintType {
    TIME_CONFLICT,
    ROOM_CAPACITY,
    PROFESSOR_AVAILABILITY,
    RESOURCE_REQUIREMENT,
    INSTITUTIONAL_POLICY,
    STUDENT_WORKLOAD,
    PREFERENCE_SATISFACTION
}

enum class ConstraintPriority {
    HARD,
    SOFT
}

data class SchedulingSolution(
    val scheduledExams: List<ScheduledExamInfo>,
    val constraintViolations: List<ConstraintViolation>,
    val qualityScore: Double,
    val isComplete: Boolean,
    val processingTimeMs: Long,
    val algorithmUsed: String?,
    val optimizationMetrics: OptimizationMetrics,
    val failureReason: String? = null
)

data class OptimizationMetrics(
    val iterationsCompleted: Int = 0,
    val solutionsEvaluated: Int = 0,
    val bestSolutionIteration: Int = 0,
    val finalTemperature: Double = 0.0,
    val convergenceRate: Double = 0.0,
    val constraintSatisfactionRate: Double = 0.0
)

data class SchedulingSessionMetrics(
    val sessionId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val strategy: SolvingStrategy,
    val problemComplexity: Double,
    val courseCount: Int,
    val roomCount: Int,
    val preferenceCount: Int,
    val constraintCount: Int,
    val processingTimeMs: Long = 0,
    val qualityScore: Double = 0.0,
    val constraintViolations: Int = 0,
    val criticalViolations: Int = 0,
    val isComplete: Boolean = false,
    val optimizationIterations: Int = 0,
    val solutionsEvaluated: Int = 0,
    val failure: Boolean = false,
    val failureReason: String? = null
)

data class AlgorithmPerformanceStats(
    val strategy: SolvingStrategy,
    val totalSessions: Int,
    val successfulSessions: Int,
    val averageQualityScore: Double,
    val averageProcessingTime: Double,
    val bestQualityScore: Double,
    val worstQualityScore: Double,
    val fastestTime: Long,
    val slowestTime: Long,
    val lastUpdated: Instant = Instant.now()
)

data class PerformanceAnalytics(
    val totalSessions: Int,
    val successfulSessions: Int,
    val failedSessions: Int,
    val averageProcessingTime: Double,
    val averageQualityScore: Double,
    val bestPerformingStrategy: SolvingStrategy?,
    val strategyPerformance: List<StrategyPerformanceSummary> = emptyList(),
    val recommendations: List<String>,
    val complexityAnalysis: ComplexityAnalysis? = null
)

data class StrategyPerformanceSummary(
    val strategy: SolvingStrategy,
    val sessionCount: Int,
    val averageQuality: Double,
    val averageTime: Double,
    val successRate: Double
)

data class ComplexityAnalysis(
    val lowComplexity: ComplexityStats?,
    val mediumComplexity: ComplexityStats?,
    val highComplexity: ComplexityStats?
)

data class ComplexityStats(
    val sessionCount: Int,
    val averageQuality: Double,
    val averageTime: Double,
    val successRate: Double
)