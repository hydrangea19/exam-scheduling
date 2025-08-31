package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ViolationSeverity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

@Service
class SchedulingPerformanceMonitor {

    private val logger = LoggerFactory.getLogger(SchedulingPerformanceMonitor::class.java)

    private val sessionMetrics = ConcurrentHashMap<String, SchedulingSessionMetrics>()
    private val algorithmPerformance = ConcurrentHashMap<SolvingStrategy, AlgorithmPerformanceStats>()
    private val sessionCounter = AtomicLong(0)


    fun startSchedulingSession(problem: SchedulingProblem, strategy: SolvingStrategy): String {
        val sessionId = "session_${sessionCounter.incrementAndGet()}_${System.currentTimeMillis()}"

        val metrics = SchedulingSessionMetrics(
            sessionId = sessionId,
            startTime = Instant.now(),
            strategy = strategy,
            problemComplexity = calculateProblemComplexity(problem),
            courseCount = problem.courses.size,
            roomCount = problem.availableRooms.size,
            preferenceCount = problem.professorPreferences.size,
            constraintCount = problem.constraints.size
        )

        sessionMetrics[sessionId] = metrics

        logger.debug("Started monitoring scheduling session: {} with strategy: {}", sessionId, strategy)
        return sessionId
    }


    fun recordSchedulingResult(
        problem: SchedulingProblem,
        solution: SchedulingSolution,
        totalProcessingTime: Long
    ) {
        val sessionId = findCurrentSession(problem)
        val sessionMetric = sessionMetrics[sessionId]

        if (sessionMetric != null) {
            val updatedMetrics = sessionMetric.copy(
                endTime = Instant.now(),
                processingTimeMs = totalProcessingTime,
                qualityScore = solution.qualityScore,
                constraintViolations = solution.constraintViolations.size,
                criticalViolations = solution.constraintViolations.count { it.severity == ViolationSeverity.CRITICAL },
                isComplete = solution.isComplete,
                optimizationIterations = solution.optimizationMetrics.iterationsCompleted,
                solutionsEvaluated = solution.optimizationMetrics.solutionsEvaluated
            )

            sessionMetrics[sessionId] = updatedMetrics
            updateAlgorithmStats(updatedMetrics)

            logger.info("Recorded scheduling result: session={}, quality={}, time={}ms, violations={}",
                sessionId, solution.qualityScore, totalProcessingTime, solution.constraintViolations.size)
        }
    }


    fun recordSchedulingFailure(exception: Exception) {
        val sessionId = "failure_${System.currentTimeMillis()}"

        val failureMetrics = SchedulingSessionMetrics(
            sessionId = sessionId,
            startTime = Instant.now(),
            endTime = Instant.now(),
            strategy = SolvingStrategy.HYBRID_APPROACH,
            problemComplexity = 0.0,
            courseCount = 0,
            roomCount = 0,
            preferenceCount = 0,
            constraintCount = 0,
            processingTimeMs = 0,
            qualityScore = 0.0,
            constraintViolations = 0,
            criticalViolations = 0,
            isComplete = false,
            failure = true,
            failureReason = exception.message
        )

        sessionMetrics[sessionId] = failureMetrics

        logger.error("Recorded scheduling failure: session={}, reason={}", sessionId, exception.message)
    }

    fun getAlgorithmPerformance(strategy: SolvingStrategy): AlgorithmPerformanceStats? {
        return algorithmPerformance[strategy]
    }


    fun getPerformanceAnalytics(): PerformanceAnalytics {
        val completedSessions = sessionMetrics.values.filter { it.endTime != null && !it.failure }
        val failedSessions = sessionMetrics.values.filter { it.failure }

        if (completedSessions.isEmpty()) {
            return PerformanceAnalytics(
                totalSessions = sessionMetrics.size,
                successfulSessions = 0,
                failedSessions = failedSessions.size,
                averageProcessingTime = 0.0,
                averageQualityScore = 0.0,
                bestPerformingStrategy = null,
                recommendations = listOf("No completed sessions to analyze")
            )
        }

        val averageTime = completedSessions.map { it.processingTimeMs }.average()
        val averageQuality = completedSessions.map { it.qualityScore }.average()

        val strategyPerformance = completedSessions
            .groupBy { it.strategy }
            .mapValues { (_, sessions) ->
                StrategyPerformanceSummary(
                    strategy = sessions.first().strategy,
                    sessionCount = sessions.size,
                    averageQuality = sessions.map { it.qualityScore }.average(),
                    averageTime = sessions.map { it.processingTimeMs }.average(),
                    successRate = sessions.count { it.isComplete }.toDouble() / sessions.size
                )
            }

        val bestStrategy = strategyPerformance.values.maxByOrNull {
            it.averageQuality * 0.6 + it.successRate * 0.3 + (1.0 / (it.averageTime / 1000.0)) * 0.1
        }

        val recommendations = generatePerformanceRecommendations(completedSessions, strategyPerformance)

        return PerformanceAnalytics(
            totalSessions = sessionMetrics.size,
            successfulSessions = completedSessions.size,
            failedSessions = failedSessions.size,
            averageProcessingTime = averageTime,
            averageQualityScore = averageQuality,
            bestPerformingStrategy = bestStrategy?.strategy,
            strategyPerformance = strategyPerformance.values.toList(),
            recommendations = recommendations,
            complexityAnalysis = analyzeComplexityPerformance(completedSessions)
        )
    }


    private fun calculateProblemComplexity(problem: SchedulingProblem): Double {
        val courseComplexity = problem.courses.size.toDouble() / 100.0 // Normalized
        val constraintComplexity = problem.constraints.size.toDouble() / 50.0
        val preferenceComplexity = problem.professorPreferences.size.toDouble() / problem.courses.size
        val roomScarcity = problem.courses.size.toDouble() / problem.availableRooms.size

        return (courseComplexity + constraintComplexity + preferenceComplexity + roomScarcity) / 4.0
    }

    private fun findCurrentSession(problem: SchedulingProblem): String {
        return sessionMetrics.values
            .filter { it.courseCount == problem.courses.size && it.endTime == null }
            .maxByOrNull { it.startTime.toEpochMilli() }
            ?.sessionId ?: "unknown"
    }

    private fun updateAlgorithmStats(metrics: SchedulingSessionMetrics) {
        val currentStats = algorithmPerformance.getOrDefault(
            metrics.strategy,
            AlgorithmPerformanceStats(
                strategy = metrics.strategy,
                totalSessions = 0,
                successfulSessions = 0,
                averageQualityScore = 0.0,
                averageProcessingTime = 0.0,
                bestQualityScore = 0.0,
                worstQualityScore = 1.0,
                fastestTime = Long.MAX_VALUE,
                slowestTime = 0L
            )
        )

        val updatedStats = currentStats.copy(
            totalSessions = currentStats.totalSessions + 1,
            successfulSessions = if (metrics.isComplete) currentStats.successfulSessions + 1 else currentStats.successfulSessions,
            averageQualityScore = ((currentStats.averageQualityScore * currentStats.totalSessions) + metrics.qualityScore) / (currentStats.totalSessions + 1),
            averageProcessingTime = ((currentStats.averageProcessingTime * currentStats.totalSessions) + metrics.processingTimeMs) / (currentStats.totalSessions + 1),
            bestQualityScore = max(currentStats.bestQualityScore, metrics.qualityScore),
            worstQualityScore = min(currentStats.worstQualityScore, metrics.qualityScore),
            fastestTime = min(currentStats.fastestTime, metrics.processingTimeMs),
            slowestTime = max(currentStats.slowestTime, metrics.processingTimeMs),
            lastUpdated = Instant.now()
        )

        algorithmPerformance[metrics.strategy] = updatedStats
    }

    private fun generatePerformanceRecommendations(
        sessions: List<SchedulingSessionMetrics>,
        strategyPerformance: Map<SolvingStrategy, StrategyPerformanceSummary>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val averageTime = sessions.map { it.processingTimeMs }.average()
        if (averageTime > 30000) {
            recommendations.add("Average processing time is high (${averageTime.toLong()}ms) - consider optimizing constraints or using faster algorithms for smaller problems")
        }

        val averageQuality = sessions.map { it.qualityScore }.average()
        if (averageQuality < 0.7) {
            recommendations.add("Average quality score is low (${String.format("%.2f", averageQuality)}) - review constraint priorities and preference handling")
        }

        val highComplexitySessions = sessions.filter { it.problemComplexity > 0.7 }
        if (highComplexitySessions.isNotEmpty()) {
            val complexityQuality = highComplexitySessions.map { it.qualityScore }.average()
            if (complexityQuality < averageQuality - 0.2) {
                recommendations.add("High-complexity problems show significantly lower quality - consider hybrid approaches or constraint relaxation")
            }
        }

        strategyPerformance.values.forEach { strategy ->
            when {
                strategy.successRate < 0.8 ->
                    recommendations.add("${strategy.strategy} has low success rate (${String.format("%.1f", strategy.successRate * 100)}%) - consider fallback mechanisms")

                strategy.averageTime > averageTime * 1.5 ->
                    recommendations.add("${strategy.strategy} is slower than average - optimize for time-critical scenarios")
            }
        }

        return recommendations.ifEmpty { listOf("Performance metrics are within acceptable ranges") }
    }

    private fun analyzeComplexityPerformance(sessions: List<SchedulingSessionMetrics>): ComplexityAnalysis {
        val low = sessions.filter { it.problemComplexity <= 0.3 }
        val medium = sessions.filter { it.problemComplexity > 0.3 && it.problemComplexity <= 0.7 }
        val high = sessions.filter { it.problemComplexity > 0.7 }

        return ComplexityAnalysis(
            lowComplexity = if (low.isNotEmpty()) ComplexityStats(
                sessionCount = low.size,
                averageQuality = low.map { it.qualityScore }.average(),
                averageTime = low.map { it.processingTimeMs }.average(),
                successRate = low.count { it.isComplete }.toDouble() / low.size
            ) else null,
            mediumComplexity = if (medium.isNotEmpty()) ComplexityStats(
                sessionCount = medium.size,
                averageQuality = medium.map { it.qualityScore }.average(),
                averageTime = medium.map { it.processingTimeMs }.average(),
                successRate = medium.count { it.isComplete }.toDouble() / medium.size
            ) else null,
            highComplexity = if (high.isNotEmpty()) ComplexityStats(
                sessionCount = high.size,
                averageQuality = high.map { it.qualityScore }.average(),
                averageTime = high.map { it.processingTimeMs }.average(),
                successRate = high.count { it.isComplete }.toDouble() / high.size
            ) else null
        )
    }
}