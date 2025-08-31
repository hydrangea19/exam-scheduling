package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.DifferenceType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ScheduleVersionType
import mk.ukim.finki.examscheduling.schedulingservice.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalTime
import java.util.*

@Service
@Transactional
class QualityScoringService(
    private val scheduleMetricsRepository: ScheduleMetricsRepository,
    private val scheduleVersionRepository: ScheduleVersionRepository,
    private val conflictAnalysisService: ConflictAnalysisService,
    private val examSessionScheduleRepository : ExamSessionScheduleRepository,
    private val scheduledExamRepository : ScheduledExamRepository,
    private val professorCommentRepository : ProfessorCommentRepository,
    private val adjustmentLogRepository : AdjustmentLogRepository,
    ) {

    private val logger = LoggerFactory.getLogger(QualityScoringService::class.java)

    fun recordSchedulingMetrics(scheduleId: UUID, metrics: SchedulingMetrics, qualityScore: Double) {
        logger.info("Recording scheduling metrics for schedule: {}, quality score: {}", scheduleId, qualityScore)

        val scheduleMetrics = ScheduleMetricsEntity(
            scheduleId = scheduleId,
            qualityScore = qualityScore,
            preferenceSatisfactionRate = metrics.preferenceSatisfactionRate,
            totalConflicts = metrics.totalConflicts,
            resolvedConflicts = metrics.resolvedConflicts,
            roomUtilizationRate = metrics.roomUtilizationRate,
            averageStudentExamsPerDay = metrics.averageStudentExamsPerDay,
            totalCoursesScheduled = metrics.totalCoursesScheduled,
            totalProfessorPreferencesConsidered = metrics.totalProfessorPreferencesConsidered,
            preferencesSatisfied = metrics.preferencesSatisfied,
            processingTimeMs = metrics.processingTimeMs,
            recordedAt = Instant.now()
        )

        scheduleMetricsRepository.save(scheduleMetrics)
    }


    fun recordFinalQualityScore(scheduleId: UUID, finalQualityScore: Double) {
        logger.info("Recording final quality score for schedule: {}, score: {}", scheduleId, finalQualityScore)

        scheduleMetricsRepository.findByScheduleIdOrderByRecordedAtDesc(scheduleId).firstOrNull()?.let { metrics ->
            val updatedMetrics = metrics.copy(
                finalQualityScore = finalQualityScore,
                finalizedAt = Instant.now()
            )
            scheduleMetricsRepository.save(updatedMetrics)
        }
    }


    fun calculateQualityScore(
        scheduledExams: List<ScheduledExam>,
        professorPreferences: List<ProfessorPreferenceInfo>,
        conflictAnalysis: ConflictAnalysisResult
    ): QualityScoreResult {
        logger.debug(
            "Calculating quality score for {} exams with {} preferences",
            scheduledExams.size, professorPreferences.size
        )

        val weights = QualityWeights(
            preferenceSatisfaction = 0.35,
            conflictMinimization = 0.25,
            resourceUtilization = 0.20,
            studentWorkloadDistribution = 0.15,
            institutionalPolicies = 0.05
        )

        val preferenceScore = calculatePreferenceSatisfactionScore(scheduledExams, professorPreferences)
        val conflictScore = calculateConflictMinimizationScore(conflictAnalysis)
        val utilizationScore = calculateResourceUtilizationScore(scheduledExams)
        val workloadScore = calculateStudentWorkloadScore(scheduledExams)
        val policyScore = calculatePolicyComplianceScore(scheduledExams)

        val overallScore = (preferenceScore * weights.preferenceSatisfaction) +
                (conflictScore * weights.conflictMinimization) +
                (utilizationScore * weights.resourceUtilization) +
                (workloadScore * weights.studentWorkloadDistribution) +
                (policyScore * weights.institutionalPolicies)

        return QualityScoreResult(
            overallScore = overallScore,
            preferenceSatisfactionScore = preferenceScore,
            conflictMinimizationScore = conflictScore,
            resourceUtilizationScore = utilizationScore,
            studentWorkloadScore = workloadScore,
            policyComplianceScore = policyScore,
            breakdown = QualityScoreBreakdown(
                totalExams = scheduledExams.size,
                totalPreferences = professorPreferences.size,
                satisfiedPreferences = calculateSatisfiedPreferences(scheduledExams, professorPreferences),
                totalConflicts = conflictAnalysis.totalConflicts,
                criticalViolations = conflictAnalysis.criticalViolations,
                recommendations = generateQualityRecommendations(preferenceScore, conflictScore, utilizationScore)
            )
        )
    }

    fun createScheduleVersion(
        scheduleId: UUID,
        versionType: ScheduleVersionType,
        versionNotes: String?,
        createdBy: String
    ): ScheduleVersionEntity {
        logger.info("Creating schedule version for: {}, type: {}", scheduleId, versionType)

        val currentSchedule = examSessionScheduleRepository.findById(scheduleId)
            .orElseThrow { IllegalArgumentException("Schedule not found: $scheduleId") }

        val currentExams = scheduledExamRepository.findByExamSessionScheduleId(scheduleId)
        val currentComments = professorCommentRepository.findByExamSessionScheduleId(scheduleId)
        val currentAdjustments = adjustmentLogRepository.findByExamSessionScheduleId(scheduleId)

        val versionNumber = scheduleVersionRepository.findByScheduleIdOrderByVersionNumberDesc(scheduleId)
            .firstOrNull()?.versionNumber?.plus(1) ?: 1

        val scheduleVersion = ScheduleVersionEntity(
            scheduleId = scheduleId,
            versionNumber = versionNumber,
            versionType = versionType,
            versionNotes = versionNotes,
            scheduleStatus = currentSchedule.status,
            snapshotData = createScheduleSnapshot(currentSchedule, currentExams, currentComments, currentAdjustments),
            createdBy = createdBy,
            createdAt = Instant.now()
        )

        return scheduleVersionRepository.save(scheduleVersion)
    }

    fun compareScheduleVersions(scheduleId: UUID, version1: Int, version2: Int): ScheduleComparisonResult {
        logger.info("Comparing schedule versions {} and {} for schedule: {}", version1, version2, scheduleId)

        val v1 = scheduleVersionRepository.findByScheduleIdAndVersionNumber(scheduleId, version1)
            ?: throw IllegalArgumentException("Version $version1 not found")

        val v2 = scheduleVersionRepository.findByScheduleIdAndVersionNumber(scheduleId, version2)
            ?: throw IllegalArgumentException("Version $version2 not found")

        return performScheduleComparison(v1, v2)
    }

    private fun calculatePreferenceSatisfactionScore(
        scheduledExams: List<ScheduledExam>,
        professorPreferences: List<ProfessorPreferenceInfo>
    ): Double {
        if (professorPreferences.isEmpty()) return 1.0

        var totalPreferences = 0
        var satisfiedPreferences = 0

        professorPreferences.forEach { preference ->
            val exam = scheduledExams.find { it.courseId == preference.courseId }
            if (exam != null) {
                totalPreferences++

                val satisfiesDate = preference.preferredDates.isEmpty() ||
                        preference.preferredDates.contains(exam.examDate)

                val satisfiesTime = preference.preferredTimeSlots.isEmpty() ||
                        preference.preferredTimeSlots.any { slot ->
                            timeSlotMatches(exam.startTime, exam.endTime, slot)
                        }

                val satisfiesRoom = preference.preferredRooms.isEmpty() ||
                        preference.preferredRooms.contains(exam.roomId)

                if (satisfiesDate && satisfiesTime && satisfiesRoom) {
                    satisfiedPreferences++
                }
            }
        }

        return if (totalPreferences > 0) satisfiedPreferences.toDouble() / totalPreferences else 1.0
    }

    private fun calculateConflictMinimizationScore(conflictAnalysis: ConflictAnalysisResult): Double {
        val maxPossibleConflicts = conflictAnalysis.totalExamSlots * (conflictAnalysis.totalExamSlots - 1) / 2
        return if (maxPossibleConflicts > 0) {
            1.0 - (conflictAnalysis.totalConflicts.toDouble() / maxPossibleConflicts)
        } else 1.0
    }

    private fun calculateResourceUtilizationScore(scheduledExams: List<ScheduledExam>): Double {
        val roomUtilization = scheduledExams
            .filter { it.roomCapacity != null && it.roomCapacity > 0 }
            .map { it.studentCount.toDouble() / it.roomCapacity!! }
            .average()

        return if (roomUtilization.isNaN()) 0.5 else minOf(roomUtilization, 1.0)
    }

    private fun calculateStudentWorkloadScore(scheduledExams: List<ScheduledExam>): Double {
        val examsByDate = scheduledExams.groupBy { it.examDate }

        val workloadScores = examsByDate.map { (date, exams) ->
            val totalStudentExams = exams.sumOf { it.studentCount }
            val averagePerExam = if (exams.isNotEmpty()) totalStudentExams.toDouble() / exams.size else 0.0

            val densityPenalty = when {
                exams.size <= 2 -> 1.0
                exams.size <= 4 -> 0.8
                exams.size <= 6 -> 0.6
                else -> 0.4
            }

            densityPenalty
        }

        return workloadScores.average().takeIf { !it.isNaN() } ?: 1.0
    }

    private fun calculatePolicyComplianceScore(scheduledExams: List<ScheduledExam>): Double {
        var violations = 0
        var totalChecks = 0

        scheduledExams.forEach { exam ->
            totalChecks++

            val duration = java.time.Duration.between(exam.startTime, exam.endTime)
            if (duration.toMinutes() < 120) {
                violations++
            }

            if (exam.startTime.isBefore(LocalTime.of(8, 0)) ||
                exam.endTime.isAfter(LocalTime.of(20, 0))
            ) {
                violations++
            }
        }

        return if (totalChecks > 0) {
            1.0 - (violations.toDouble() / totalChecks)
        } else 1.0
    }

    private fun calculateSatisfiedPreferences(
        scheduledExams: List<ScheduledExam>,
        professorPreferences: List<ProfessorPreferenceInfo>
    ): Int {
        return professorPreferences.count { preference ->
            val exam = scheduledExams.find { it.courseId == preference.courseId }
            exam != null && isPreferenceSatisfied(exam, preference)
        }
    }

    private fun isPreferenceSatisfied(exam: ScheduledExam, preference: ProfessorPreferenceInfo): Boolean {
        val satisfiesDate = preference.preferredDates.isEmpty() ||
                preference.preferredDates.contains(exam.examDate)

        val satisfiesTime = preference.preferredTimeSlots.isEmpty() ||
                preference.preferredTimeSlots.any { slot ->
                    timeSlotMatches(exam.startTime, exam.endTime, slot)
                }

        val satisfiesRoom = preference.preferredRooms.isEmpty() ||
                preference.preferredRooms.contains(exam.roomId)

        return satisfiesDate && satisfiesTime && satisfiesRoom
    }

    private fun timeSlotMatches(examStart: LocalTime, examEnd: LocalTime, preference: TimeSlotPreference): Boolean {
        val flexibilityMinutes = 30L
        val examStartTime = examStart.minusMinutes(flexibilityMinutes)
        val examEndTime = examEnd.plusMinutes(flexibilityMinutes)

        return !preference.startTime.isAfter(examEndTime) && !preference.endTime.isBefore(examStartTime)
    }

    private fun generateQualityRecommendations(
        preferenceScore: Double,
        conflictScore: Double,
        utilizationScore: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (preferenceScore < 0.7) {
            recommendations.add("Consider reviewing professor preferences - satisfaction rate is below 70%")
        }

        if (conflictScore < 0.8) {
            recommendations.add("High number of scheduling conflicts detected - review time slot allocation")
        }

        if (utilizationScore < 0.6) {
            recommendations.add("Room utilization is suboptimal - consider room reallocation")
        }

        if (utilizationScore > 0.95) {
            recommendations.add("Rooms are overutilized - consider additional room capacity")
        }

        return recommendations
    }

    private fun createScheduleSnapshot(
        schedule: ExamSessionSchedule,
        exams: List<ScheduledExam>,
        comments: List<ProfessorComment>,
        adjustments: List<AdjustmentLog>
    ): String {
        val snapshot = mapOf(
            "schedule" to mapOf(
                "id" to schedule.id.toString(),
                "examSessionPeriodId" to schedule.examSessionPeriodId,
                "academicYear" to schedule.academicYear,
                "examSession" to schedule.examSession,
                "status" to schedule.status.name,
                "startDate" to schedule.startDate.toString(),
                "endDate" to schedule.endDate.toString(),
                "createdAt" to schedule.createdAt.toString(),
                "updatedAt" to schedule.updatedAt?.toString()
            ),
            "exams" to exams.map { exam ->
                mapOf(
                    "scheduledExamId" to exam.scheduledExamId,
                    "courseId" to exam.courseId,
                    "courseName" to exam.courseName,
                    "examDate" to exam.examDate.toString(),
                    "startTime" to exam.startTime.toString(),
                    "endTime" to exam.endTime.toString(),
                    "roomId" to exam.roomId,
                    "roomName" to exam.roomName,
                    "roomCapacity" to exam.roomCapacity,
                    "studentCount" to exam.studentCount,
                    "mandatoryStatus" to exam.mandatoryStatus.name,
                    "professorIds" to exam.professorIds.toList()
                )
            },
            "commentsCount" to comments.size,
            "adjustmentsCount" to adjustments.size,
            "snapshotCreatedAt" to Instant.now().toString()
        )

        return com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(snapshot)
    }

    private fun performScheduleComparison(
        v1: ScheduleVersionEntity,
        v2: ScheduleVersionEntity
    ): ScheduleComparisonResult {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        val snapshot1 = mapper.readValue(v1.snapshotData, Map::class.java)
        val snapshot2 = mapper.readValue(v2.snapshotData, Map::class.java)

        val differences = mutableListOf<ScheduleDifference>()

        compareScheduleProperties(snapshot1, snapshot2, differences)

        compareExams(snapshot1, snapshot2, differences)

        return ScheduleComparisonResult(
            scheduleId = v1.scheduleId,
            version1 = v1.versionNumber,
            version2 = v2.versionNumber,
            differences = differences,
            comparedAt = Instant.now()
        )
    }

    private fun compareScheduleProperties(
        snapshot1: Map<*, *>,
        snapshot2: Map<*, *>,
        differences: MutableList<ScheduleDifference>
    ) {
        val schedule1 = snapshot1["schedule"] as? Map<*, *> ?: return
        val schedule2 = snapshot2["schedule"] as? Map<*, *> ?: return

        listOf("status", "startDate", "endDate").forEach { property ->
            val value1 = schedule1[property]
            val value2 = schedule2[property]

            if (value1 != value2) {
                differences.add(
                    ScheduleDifference(
                        type = DifferenceType.SCHEDULE_PROPERTY,
                        property = property,
                        oldValue = value1?.toString(),
                        newValue = value2?.toString(),
                        description = "Schedule $property changed from '$value1' to '$value2'"
                    )
                )
            }
        }
    }

    private fun compareExams(
        snapshot1: Map<*, *>,
        snapshot2: Map<*, *>,
        differences: MutableList<ScheduleDifference>
    ) {
        val exams1 = (snapshot1["exams"] as? List<Map<*, *>>) ?: emptyList()
        val exams2 = (snapshot2["exams"] as? List<Map<*, *>>) ?: emptyList()

        val examMap1 = exams1.associateBy { it["scheduledExamId"] as String }
        val examMap2 = exams2.associateBy { it["scheduledExamId"] as String }

        examMap2.keys.subtract(examMap1.keys).forEach { examId ->
            val exam = examMap2[examId]!!
            differences.add(
                ScheduleDifference(
                    type = DifferenceType.EXAM_ADDED,
                    examId = examId,
                    courseId = exam["courseId"] as? String,
                    description = "Added exam for course ${exam["courseId"]} on ${exam["examDate"]} at ${exam["startTime"]}"
                )
            )
        }

        examMap1.keys.subtract(examMap2.keys).forEach { examId ->
            val exam = examMap1[examId]!!
            differences.add(
                ScheduleDifference(
                    type = DifferenceType.EXAM_REMOVED,
                    examId = examId,
                    courseId = exam["courseId"] as? String,
                    description = "Removed exam for course ${exam["courseId"]}"
                )
            )
        }

        examMap1.keys.intersect(examMap2.keys).forEach { examId ->
            val exam1 = examMap1[examId]!!
            val exam2 = examMap2[examId]!!

            compareExamProperties(exam1, exam2, examId, differences)
        }
    }

    private fun compareExamProperties(
        exam1: Map<*, *>,
        exam2: Map<*, *>,
        examId: String,
        differences: MutableList<ScheduleDifference>
    ) {
        val properties = listOf("examDate", "startTime", "endTime", "roomId", "roomName", "roomCapacity")

        properties.forEach { property ->
            val value1 = exam1[property]
            val value2 = exam2[property]

            if (value1 != value2) {
                differences.add(
                    ScheduleDifference(
                        type = DifferenceType.EXAM_MODIFIED,
                        examId = examId,
                        courseId = exam1["courseId"] as? String,
                        property = property,
                        oldValue = value1?.toString(),
                        newValue = value2?.toString(),
                        description = "Exam ${exam1["courseId"]} $property changed from '$value1' to '$value2'"
                    )
                )
            }
        }
    }

    fun getLatestMetrics(scheduleId: UUID): ScheduleMetricsEntity? {
        logger.debug("Retrieving latest metrics for schedule: {}", scheduleId)
        return scheduleMetricsRepository.findByScheduleIdOrderByRecordedAtDesc(scheduleId).firstOrNull()
    }


    fun getAllMetrics(scheduleId: UUID): List<ScheduleMetricsEntity> {
        logger.debug("Retrieving all metrics for schedule: {}", scheduleId)
        return scheduleMetricsRepository.findByScheduleIdOrderByRecordedAtDesc(scheduleId)
    }


    fun calculateQualityTrends(scheduleId: UUID): QualityTrendsResult {
        logger.debug("Calculating quality trends for schedule: {}", scheduleId)

        val metrics = scheduleMetricsRepository.findByScheduleIdOrderByRecordedAtDesc(scheduleId)

        if (metrics.size < 2) {
            return QualityTrendsResult(
                scheduleId = scheduleId,
                dataPoints = metrics.size,
                overallTrend = TrendDirection.STABLE,
                qualityScoreTrend = 0.0,
                preferenceSatisfactionTrend = 0.0,
                conflictResolutionTrend = 0.0,
                recommendations = if (metrics.isEmpty()) {
                    listOf("No metrics available for trend analysis")
                } else {
                    listOf("Insufficient data for trend analysis - need at least 2 data points")
                }
            )
        }

        val latest = metrics.first()
        val previous = metrics[1]

        val qualityScoreChange = latest.qualityScore - previous.qualityScore
        val preferenceSatisfactionChange = latest.preferenceSatisfactionRate - previous.preferenceSatisfactionRate
        val conflictResolutionChange = (latest.resolvedConflicts.toDouble() / (latest.totalConflicts + 1)) -
                (previous.resolvedConflicts.toDouble() / (previous.totalConflicts + 1))

        val overallTrend = when {
            qualityScoreChange > 0.05 -> TrendDirection.IMPROVING
            qualityScoreChange < -0.05 -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        val recommendations = mutableListOf<String>()

        if (qualityScoreChange < -0.1) {
            recommendations.add("Quality score has declined significantly - review recent changes")
        }
        if (preferenceSatisfactionChange < -0.1) {
            recommendations.add("Professor preference satisfaction has decreased - consider adjusting algorithm")
        }
        if (conflictResolutionChange < -0.1) {
            recommendations.add("Conflict resolution rate has worsened - review scheduling constraints")
        }

        return QualityTrendsResult(
            scheduleId = scheduleId,
            dataPoints = metrics.size,
            overallTrend = overallTrend,
            qualityScoreTrend = qualityScoreChange,
            preferenceSatisfactionTrend = preferenceSatisfactionChange,
            conflictResolutionTrend = conflictResolutionChange,
            recommendations = recommendations.ifEmpty { listOf("Quality metrics are stable") }
        )
    }

    /*fun compareAcrossSessionsBenchmark(academicYear: String): SessionComparisonResult {
        logger.info("Comparing quality metrics across sessions for academic year: {}", academicYear)

        val benchmarks = scheduleQualityBenchmarkRepository.findByExamSessionPeriodId("${academicYear}%")
            .groupBy { it.examSessionPeriodId }

        val sessionStats = benchmarks.map { (sessionId, sessionBenchmarks) ->
            val latest = sessionBenchmarks.maxByOrNull { it.recordedAt }
            SessionQualityStats(
                examSessionPeriodId = sessionId,
                averageQualityScore = sessionBenchmarks.map { it.qualityScore }.average(),
                bestQualityScore = sessionBenchmarks.maxOfOrNull { it.qualityScore } ?: 0.0,
                averagePreferenceSatisfaction = sessionBenchmarks.map { it.preferenceSatisfactionRate }.average(),
                averageRoomUtilization = sessionBenchmarks.map { it.roomUtilizationEfficiency }.average(),
                totalGenerations = sessionBenchmarks.size,
                latestUpdate = latest?.recordedAt
            )
        }

        return SessionComparisonResult(
            academicYear = academicYear,
            sessionStats = sessionStats,
            bestOverallSession = sessionStats.maxByOrNull { it.averageQualityScore },
            recommendations = generateCrossSessionRecommendations(sessionStats)
        )
    }*/

    private fun generateCrossSessionRecommendations(stats: List<SessionQualityStats>): List<String> {
        val recommendations = mutableListOf<String>()

        val avgQuality = stats.map { it.averageQualityScore }.average()
        val bestSession = stats.maxByOrNull { it.averageQualityScore }
        val worstSession = stats.minByOrNull { it.averageQualityScore }

        if (bestSession != null && worstSession != null && bestSession.averageQualityScore - worstSession.averageQualityScore > 0.2) {
            recommendations.add("Significant quality variation detected between sessions - analyze ${bestSession.examSessionPeriodId} practices")
        }

        stats.filter { it.averagePreferenceSatisfaction < 0.6 }.forEach { session ->
            recommendations.add("Session ${session.examSessionPeriodId} has low preference satisfaction - review professor requirements")
        }

        return recommendations.ifEmpty { listOf("All sessions performing within acceptable ranges") }
    }
}