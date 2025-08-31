package mk.ukim.finki.examscheduling.schedulingservice.domain

import jakarta.validation.Valid
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.sharedsecurity.utilities.SecurityUtils
import org.aspectj.weaver.tools.cache.CacheStatistics
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import java.util.concurrent.CompletableFuture

data class IntegratedSchedulingData(
    val examSessionPeriodId: String,
    val courses: Map<String, MergedCourseInfo>,
    val preferences: List<ValidatedPreferenceInfo>,
    val resources: List<OptimizedRoomInfo>,
    val dataQuality: DataQualityMetrics,
    val integrationMetadata: DataIntegrationMetadata
)

data class AccreditationIntegrationResult(
    val courses: Map<String, CourseAccreditationInfo>,
    val issues: List<DataIntegrationIssue>,
    val sourceMetadata: Map<String, Any>,
    val processedAt: Instant
)

data class EnrollmentIntegrationResult(
    val enrollments: Map<String, CourseEnrollmentInfo>,
    val issues: List<DataIntegrationIssue>,
    val sourceMetadata: Map<String, Any>,
    val processedAt: Instant
)

data class PreferencesIntegrationResult(
    val preferences: List<ProfessorPreferenceInfo>,
    val issues: List<DataIntegrationIssue>,
    val sourceMetadata: Map<String, Any>,
    val processedAt: Instant
)

data class ResourceAvailabilityResult(
    val resources: List<RoomInfo>,
    val availabilityPeriod: String,
    val generatedAt: Instant
)

data class MergedCourseInfo(
    val courseId: String,
    val courseName: String,
    val studentCount: Int,
    val professorIds: Set<String>,
    val mandatoryStatus: MandatoryStatus,
    val credits: Int,
    val prerequisites: Set<String>,
    val estimatedDuration: Int,
    val dataQuality: DataQuality
)

data class ValidatedPreferenceInfo(
    val preferenceInfo: ProfessorPreferenceInfo,
    val courseExists: Boolean,
    val professorAuthorized: Boolean,
    val validationScore: Double
)

data class OptimizedRoomInfo(
    val roomInfo: RoomInfo,
    val suitableCourseCount: Int,
    val utilizationPotential: Double,
    val priority: Double
)

data class DataIntegrationIssue(
    val type: String,
    val source: String,
    val entityId: String,
    val description: String,
    val severity: IssueSeverity
)

data class DataQualityMetrics(
    val overallScore: Double,
    val completenessScore: Double,
    val freshnessScore: Double,
    val totalIssues: Int,
    val criticalIssues: Int,
    val dataSourceHealth: Map<String, Double>
)

data class DataIntegrationMetadata(
    val fetchDuration: Long,
    val sourceCount: Int,
    val integratedAt: Instant
)

data class DataIntegrityValidationResult(
    val isValid: Boolean,
    val overallConsistencyScore: Double,
)

enum class IssueSeverity {
    CRITICAL, ERROR, WARNING, INFO
}

enum class DataQuality {
    HIGH, MEDIUM, LOW
}

data class CourseRequirements(
    val courseId: String,
    val minimumCapacity: Int,
    val preferredCapacity: Int,
    val requiredEquipment: Set<String>,
    val accessibilityRequired: Boolean,
    val roomPreferences: List<String>,
    val timeConstraints: List<TimeConstraint>,
    val priority: Int,
    val specialRequirements: List<String>
)

data class RoomCapabilities(
    val roomId: String,
    val roomName: String,
    val capacity: Int,
    val availableEquipment: Set<String>,
    val isAccessible: Boolean,
    val location: String,
    val availableTimeSlots: List<TimeSlot>,
    val utilizationScore: Double,
    val flexibilityScore: Double
)

data class RoomAllocation(
    val courseId: String,
    val roomId: String,
    val roomName: String,
    val allocatedCapacity: Int,
    val requiredCapacity: Int,
    val utilizationRate: Double,
    val timeSlots: List<TimeSlot>,
    val allocationScore: Double,
    val constraints: List<String>
)

data class TimeConstraint(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val dayOfWeek: Int?,
    val priority: Int
)

data class RoomAllocationResult(
    val allocations: List<RoomAllocation>,
    val unallocatedCourses: List<String>,
    val metrics: AllocationMetrics,
    val processingTime: Long,
    val qualityScore: Double
)

data class AllocationMetrics(
    val totalCourses: Int,
    val successfulAllocations: Int,
    val successRate: Double,
    val averageRoomUtilization: Double,
    val totalCapacityWaste: Int,
    val roomDistribution: Map<String, Int>
)

data class ResourceChangeRequest(
    val requestId: String,
    val targetResourceId: String,
    val resourceType: ResourceType,
    val changeType: ResourceChangeType,
    val requiredCapacity: Int
)

data class ResourceImpact(
    val createsBottleneck: Boolean,
    val capacityOverflow: Int,
    val affectedCourses: List<String>
)

data class ResourceBottleneck(
    val resourceId: String,
    val resourceType: ResourceType,
    val overallocation: Int,
    val affectedCourses: List<String>,
    val severity: BottleneckSeverity
)

data class CapacityAnalysisResult(
    val impactAnalysis: Map<String, ResourceImpact>,
    val identifiedBottlenecks: List<ResourceBottleneck>,
    val overallCapacityUtilization: Double,
    val recommendations: List<String>,
    val feasibilityScore: Double
)

data class ResourceOptimizationResult(
    val originalUtilization: Double,
    val optimizedUtilization: Double,
    val improvementPercent: Double,
    val optimizedSchedule: List<ScheduledExamInfo>,
    val appliedStrategies: List<OptimizationStrategy>,
    val processingTime: Long,
    val qualityImprovement: Double
)

data class UtilizationAnalysis(val overallUtilization: Double)
data class OptimizationOpportunity(val type: String, val impact: Double)
data class OptimizationStrategy(val name: String, val parameters: Map<String, Any>)
// data class OptimizationMetrics(val optimizedUtilization: Double, val improvementPercent: Double, val qualityImprovement: Double)

enum class ResourceType { ROOM, EQUIPMENT, TIME_SLOT }
enum class ResourceChangeType { ALLOCATION, DEALLOCATION, MODIFICATION }
enum class BottleneckSeverity { CRITICAL, HIGH, MEDIUM, LOW }


data class GenerateAdvancedScheduleCommand(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val generatedBy: String,
    val institutionalConstraints: InstitutionalConstraints?
)

data class ApplyGeneratedScheduleCommand(
    @TargetAggregateIdentifier
    val scheduleId: UUID,
    val schedulingResult: SchedulingResult,
    val resourceAllocations: RoomAllocationResult,
    val dataQualityMetrics: DataQualityMetrics,
    val generationMetadata: GenerationMetadata,
    val generatedBy: String
)

data class ReallocateResourcesCommand(
    val scheduleId: UUID,
    val currentAllocations: Map<String, RoomAllocation>,
    val proposedChanges: List<ResourceChangeRequest>,
    val availableRooms: List<RoomInfo>,
    val requestedBy: String
)

data class SynchronizeExternalDataCommand(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val synchronizedBy: String
)

data class ValidateScheduleFeasibilityCommand(
    val scheduleId: UUID,
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val validationCriteria: FeasibilityValidationCriteria,
    val validatedBy: String
)

data class UpdateDataQualityMetricsCommand(
    @org.axonframework.modelling.command.TargetAggregateIdentifier
    val scheduleId: UUID,
    val dataQualityMetrics: DataQualityMetrics,
    val lastSynchronized: java.time.Instant,
    val synchronizedBy: String
)


sealed class ScheduleGenerationResult {
    data class Success(
        val scheduleId: UUID,
        val schedulingResult: SchedulingResult,
        val resourceAllocations: RoomAllocationResult,
        val dataQuality: DataQualityMetrics
    ) : ScheduleGenerationResult()

    data class Failure(
        val scheduleId: UUID,
        val errorMessage: String,
        val dataQuality: DataQualityMetrics?
    ) : ScheduleGenerationResult()

    companion object {
        fun success(
            scheduleId: UUID,
            schedulingResult: SchedulingResult,
            resourceAllocations: RoomAllocationResult,
            dataQuality: DataQualityMetrics
        ) = Success(scheduleId, schedulingResult, resourceAllocations, dataQuality)

        fun failure(scheduleId: UUID, errorMessage: String, dataQuality: DataQualityMetrics?) =
            Failure(scheduleId, errorMessage, dataQuality)
    }
}

sealed class ResourceReallocationResult {
    data class Success(
        val scheduleId: UUID,
        val optimizationResult: ResourceOptimizationResult,
        val capacityAnalysis: CapacityAnalysisResult
    ) : ResourceReallocationResult()

    data class Infeasible(
        val scheduleId: UUID,
        val bottlenecks: List<ResourceBottleneck>,
        val recommendations: List<String>
    ) : ResourceReallocationResult()

    data class Failure(
        val scheduleId: UUID,
        val errorMessage: String
    ) : ResourceReallocationResult()

    companion object {
        fun success(scheduleId: UUID, result: ResourceOptimizationResult, analysis: CapacityAnalysisResult) =
            Success(scheduleId, result, analysis)

        fun infeasible(scheduleId: UUID, bottlenecks: List<ResourceBottleneck>, recommendations: List<String>) =
            Infeasible(scheduleId, bottlenecks, recommendations)

        fun failure(scheduleId: UUID, errorMessage: String) = Failure(scheduleId, errorMessage)
    }
}

sealed class DataSynchronizationResult {
    data class WithRegeneration(
        val scheduleId: UUID,
        val dataChanges: DataChangesSummary,
        val dataQuality: DataQualityMetrics
    ) : DataSynchronizationResult()

    data class DataOnly(
        val scheduleId: UUID,
        val dataChanges: DataChangesSummary,
        val dataQuality: DataQualityMetrics
    ) : DataSynchronizationResult()

    data class Failure(
        val scheduleId: UUID,
        val errorMessage: String
    ) : DataSynchronizationResult()

    companion object {
        fun withRegeneration(scheduleId: UUID, changes: DataChangesSummary, quality: DataQualityMetrics) =
            WithRegeneration(scheduleId, changes, quality)

        fun dataOnly(scheduleId: UUID, changes: DataChangesSummary, quality: DataQualityMetrics) =
            DataOnly(scheduleId, changes, quality)

        fun failure(scheduleId: UUID, errorMessage: String) = Failure(scheduleId, errorMessage)
    }
}

data class FeasibilityValidationResult(
    val scheduleId: UUID,
    val isFeasible: Boolean,
    val feasibilityScore: Double,
    val validationResults: Map<String, Boolean>,
    val recommendations: List<String>,
    val dataQuality: DataQualityMetrics,
    val validatedAt: java.time.Instant,
    val validatedBy: String
) {
    companion object {
        fun failure(scheduleId: UUID, errorMessage: String) = FeasibilityValidationResult(
            scheduleId = scheduleId,
            isFeasible = false,
            feasibilityScore = 0.0,
            validationResults = emptyMap(),
            recommendations = listOf("Validation failed: $errorMessage"),
            dataQuality = DataQualityMetrics(0.0, 0.0, 0.0, 0, 0, emptyMap()),
            validatedAt = Instant.now(),
            validatedBy = "system"
        )
    }
}

data class GenerationMetadata(
    val externalDataSources: Int,
    val dataFreshness: Double,
    val algorithmUsed: String?,
    val processingTime: Long
)

data class DataChangesSummary(
    val hasSignificantChanges: Boolean,
    val courseChanges: Int,
    val enrollmentChanges: Int,
    val preferenceChanges: Int,
    val resourceChanges: Int,
    val changeDetails: List<String>
)

data class FeasibilityValidationCriteria(
    val checkCapacityConstraints: Boolean = true,
    val checkResourceAvailability: Boolean = true,
    val checkPreferenceCompatibility: Boolean = true,
    val checkInstitutionalPolicies: Boolean = true,
    val minimumQualityThreshold: Double = 0.6
)

data class FeasibilityAnalysisResult(
    val overallFeasibility: Boolean,
    val feasibilityScore: Double,
    val detailedResults: Map<String, Boolean>
)

data class DataIntegrationRequest(
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String
)

data class DataSyncRequest(
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class CourseValidationRequest(
    val courseId: String,
    val courseName: String,
    val credits: Int,
    val mandatory: Boolean,
    val professors: List<String>
) {
    fun toCourseAccreditationInfo() = CourseAccreditationInfo(
        courseId = courseId,
        courseName = courseName,
        mandatoryStatus = if (mandatory) MandatoryStatus.MANDATORY else MandatoryStatus.ELECTIVE,
        credits = credits,
        professorIds = professors.toSet()
    )
}

data class EnrollmentValidationRequest(
    val courseId: String,
    val studentCount: Int
) {
    fun toCourseEnrollmentInfo() = CourseEnrollmentInfo(courseId, studentCount)
}

data class PreferenceValidationRequest(
    val preferenceId: String,
    val professorId: String,
    val courseId: String,
    val preferredDates: List<String> = emptyList(),
    val priority: Int = 3
) {
    fun toProfessorPreferenceInfo() = ProfessorPreferenceInfo(
        preferenceId = preferenceId,
        professorId = professorId,
        courseId = courseId,
        preferredDates = preferredDates.mapNotNull {
            try { LocalDate.parse(it) } catch (e: Exception) { null }
        },
        priority = priority
    )
}

data class IntegrityValidationRequest(
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String
)

data class ResourceReallocationRequest(
    val currentAllocations: Map<String, RoomAllocation>,
    val proposedChanges: List<ResourceChangeRequest>,
    val availableRooms: List<RoomInfo>
)

data class FeasibilityValidationRequest(
    val examSessionPeriodId: String,
    val academicYear: String,
    val examSession: String,
    val validationCriteria: FeasibilityValidationCriteria
)


data class IntegratedDataResponse(
    val examSessionPeriodId: String,
    val courseCount: Int,
    val preferenceCount: Int,
    val resourceCount: Int,
    val dataQuality: DataQualityResponse,
    val integrationMetadata: IntegrationMetadataResponse,
    val integratedAt: Instant,
    val issues: List<String>,
    val recommendations: List<String>
) {
    companion object {
        fun error(sessionId: String, message: String) = IntegratedDataResponse(
            examSessionPeriodId = sessionId,
            courseCount = 0,
            preferenceCount = 0,
            resourceCount = 0,
            dataQuality = DataQualityResponse.empty(),
            integrationMetadata = IntegrationMetadataResponse.empty(),
            integratedAt = Instant.now(),
            issues = listOf(message),
            recommendations = emptyList()
        )
    }
}

sealed class DataSyncResponse {
    abstract val scheduleId: UUID

    data class WithRegeneration(
        override val scheduleId: UUID,
        val dataChanges: DataChangesSummary,
        val dataQuality: DataQualityResponse,
        val regenerationTriggered: Boolean = true
    ) : DataSyncResponse()

    data class DataOnly(
        override val scheduleId: UUID,
        val dataChanges: DataChangesSummary,
        val dataQuality: DataQualityResponse,
        val regenerationTriggered: Boolean = false
    ) : DataSyncResponse()

    data class Failure(
        override val scheduleId: UUID,
        val errorMessage: String
    ) : DataSyncResponse()

    companion object {
        fun withRegeneration(scheduleId: UUID, changes: DataChangesSummary, quality: DataQualityResponse) =
            WithRegeneration(scheduleId, changes, quality)
        fun dataOnly(scheduleId: UUID, changes: DataChangesSummary, quality: DataQualityResponse) =
            DataOnly(scheduleId, changes, quality)
        fun failure(scheduleId: UUID, message: String) = Failure(scheduleId, message)
    }
}

data class ValidationResponse(
    val isValid: Boolean,
    val entityId: String,
    val entityType: String,
    val validationDetails: Map<String, Any>,
    val validatedAt: Instant
)

data class IntegrityValidationResponse(
    val examSessionPeriodId: String,
    val isValid: Boolean,
    val consistencyScore: Double,
    val dataQuality: DataQualityResponse,
    val validatedAt: Instant
) {
    companion object {
        fun error(sessionId: String, message: String) = IntegrityValidationResponse(
            examSessionPeriodId = sessionId,
            isValid = false,
            consistencyScore = 0.0,
            dataQuality = DataQualityResponse.empty(),
            validatedAt = Instant.now()
        )
    }
}

data class DataQualityResponse(
    val scheduleId: UUID? = null,
    val overallScore: Double,
    val completenessScore: Double,
    val freshnessScore: Double,
    val totalIssues: Int,
    val criticalIssues: Int,
    val sourceHealth: Map<String, Double>,
    val lastUpdated: Instant,
    val recommendations: List<String>
) {
    companion object {
        fun empty() = DataQualityResponse(
            overallScore = 0.0,
            completenessScore = 0.0,
            freshnessScore = 0.0,
            totalIssues = 0,
            criticalIssues = 0,
            sourceHealth = emptyMap(),
            lastUpdated = Instant.now(),
            recommendations = emptyList()
        )
    }
}

data class DataQualityTrendsResponse(
    val timeframe: String,
    val averageQualityScore: Double,
    val trendDirection: String,
    val dataPoints: List<Map<String, Any>>,
    val insights: List<String>,
    val generatedAt: Instant
)

data class CacheStatisticsResponse(
    val accreditationCacheSize: Int,
    val enrollmentCacheSize: Int,
    val resourceCacheSize: Int,
    val totalHits: Long,
    val totalMisses: Long,
    val hitRate: Double,
    val lastCleanup: Instant,
    val recommendations: List<String>
)

data class CacheInvalidationResponse(val message: String)

sealed class ResourceReallocationResponse {
    abstract val scheduleId: UUID

    data class Success(
        override val scheduleId: UUID,
        val optimizationResult: ResourceOptimizationResult,
        val capacityAnalysis: CapacityAnalysisResult
    ) : ResourceReallocationResponse()

    data class Infeasible(
        override val scheduleId: UUID,
        val bottlenecks: List<ResourceBottleneck>,
        val recommendations: List<String>
    ) : ResourceReallocationResponse()

    data class Failure(
        override val scheduleId: UUID,
        val errorMessage: String
    ) : ResourceReallocationResponse()

    companion object {
        fun success(scheduleId: UUID, result: ResourceOptimizationResult, analysis: CapacityAnalysisResult) =
            Success(scheduleId, result, analysis)
        fun infeasible(scheduleId: UUID, bottlenecks: List<ResourceBottleneck>, recommendations: List<String>) =
            Infeasible(scheduleId, bottlenecks, recommendations)
        fun failure(scheduleId: UUID, message: String) = Failure(scheduleId, message)
    }
}

data class FeasibilityResponse(
    val scheduleId: UUID,
    val isFeasible: Boolean,
    val feasibilityScore: Double,
    val validationResults: Map<String, Boolean>,
    val recommendations: List<String>,
    val dataQuality: DataQualityResponse,
    val validatedAt: Instant
) {
    companion object {
        fun error(scheduleId: UUID, message: String?) = FeasibilityResponse(
            scheduleId = scheduleId,
            isFeasible = false,
            feasibilityScore = 0.0,
            validationResults = emptyMap(),
            recommendations = listOf("Validation failed: ${message ?: "Unknown error"}"),
            dataQuality = DataQualityResponse.empty(),
            validatedAt = Instant.now()
        )
    }
}

data class IntegrationMetadataResponse(
    val fetchDuration: Long,
    val sourceCount: Int,
    val integratedAt: Instant
) {
    companion object {
        fun empty() = IntegrationMetadataResponse(0L,  0, Instant.now())
    }
}

fun DataQualityMetrics.toResponse() = DataQualityResponse(
    overallScore = overallScore,
    completenessScore = completenessScore,
    freshnessScore = freshnessScore,
    totalIssues = totalIssues,
    criticalIssues = criticalIssues,
    sourceHealth = dataSourceHealth,
    lastUpdated = Instant.now(),
    recommendations = emptyList()
)

fun DataIntegrationMetadata.toResponse() = IntegrationMetadataResponse(
    fetchDuration = fetchDuration,
    sourceCount = sourceCount,
    integratedAt = integratedAt
)