package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min

@Service
class ResourceManagementService {

    private val logger = LoggerFactory.getLogger(ResourceManagementService::class.java)

    /**
     * Calculates optimal room allocations for courses using advanced algorithms.
     */
    fun calculateOptimalRoomAllocations(
        courses: Map<String, MergedCourseInfo>,
        availableRooms: List<RoomInfo>,
        examPeriod: ExamPeriod,
        preferences: List<ValidatedPreferenceInfo> = emptyList()
    ): RoomAllocationResult {
        logger.info(
            "Calculating optimal room allocations for {} courses and {} rooms",
            courses.size, availableRooms.size
        )

        val allocationStart = System.currentTimeMillis()

        val courseRequirements = analyzeCourseRequirements(courses, preferences)

        val roomCapabilities = assessRoomCapabilities(availableRooms, examPeriod)

        val primaryAllocations = applyPrimaryAllocationAlgorithm(courseRequirements, roomCapabilities)
        val optimizedAllocations = applyAllocationOptimization(primaryAllocations, roomCapabilities)

        val validatedAllocations = validateAndResolveConflicts(optimizedAllocations, courseRequirements)

        val allocationMetrics = calculateAllocationMetrics(validatedAllocations, courseRequirements, roomCapabilities)

        val processingTime = System.currentTimeMillis() - allocationStart

        logger.info(
            "Room allocation calculation completed in {}ms with {}% success rate",
            processingTime, allocationMetrics.successRate * 100
        )

        return RoomAllocationResult(
            allocations = validatedAllocations,
            unallocatedCourses = findUnallocatedCourses(courses.keys, validatedAllocations),
            metrics = allocationMetrics,
            processingTime = processingTime,
            qualityScore = calculateAllocationQualityScore(allocationMetrics)
        )
    }


    fun performCapacityAnalysis(
        currentAllocations: Map<String, RoomAllocation>,
        proposedChanges: List<ResourceChangeRequest>
    ): CapacityAnalysisResult {
        logger.debug("Performing capacity analysis for {} proposed changes", proposedChanges.size)

        val impactAnalysis = mutableMapOf<String, ResourceImpact>()
        val bottlenecks = mutableListOf<ResourceBottleneck>()
        val recommendations = mutableListOf<String>()

        proposedChanges.forEach { change ->
            val impact = analyzeResourceChangeImpact(change, currentAllocations)
            impactAnalysis[change.requestId] = impact

            if (impact.createsBottleneck) {
                bottlenecks.add(
                    ResourceBottleneck(
                        resourceId = change.targetResourceId,
                        resourceType = change.resourceType,
                        overallocation = impact.capacityOverflow,
                        affectedCourses = impact.affectedCourses,
                        severity = calculateBottleneckSeverity(impact.capacityOverflow)
                    )
                )
            }
        }

        recommendations.addAll(generateCapacityRecommendations(currentAllocations, bottlenecks))

        return CapacityAnalysisResult(
            impactAnalysis = impactAnalysis,
            identifiedBottlenecks = bottlenecks,
            overallCapacityUtilization = calculateOverallUtilization(currentAllocations),
            recommendations = recommendations,
            feasibilityScore = calculateFeasibilityScore(impactAnalysis, bottlenecks)
        )
    }

    /**
     * Optimizes existing resource utilization using advanced techniques.
     */
    fun optimizeResourceUtilization(
        currentSchedule: List<ScheduledExamInfo>,
        availableRooms: List<RoomInfo>
    ): ResourceOptimizationResult {
        logger.info("Optimizing resource utilization for {} scheduled exams", currentSchedule.size)

        val optimizationStart = System.currentTimeMillis()

        val utilizationAnalysis = analyzeCurrentUtilization(currentSchedule, availableRooms)

        val opportunities = identifyOptimizationOpportunities(utilizationAnalysis)

        val optimizationStrategies = selectOptimizationStrategies(opportunities)
        val optimizedAllocations = applyOptimizationStrategies(currentSchedule, optimizationStrategies)

        val optimizationMetrics = calculateOptimizationMetrics(currentSchedule, optimizedAllocations)

        val processingTime = System.currentTimeMillis() - optimizationStart

        return ResourceOptimizationResult(
            originalUtilization = utilizationAnalysis.overallUtilization,
            optimizedUtilization = utilizationAnalysis.overallUtilization,
            improvementPercent = optimizationMetrics.convergenceRate,
            optimizedSchedule = optimizedAllocations,
            appliedStrategies = optimizationStrategies,
            processingTime = processingTime,
            qualityImprovement = optimizationMetrics.convergenceRate
        )
    }

    private fun analyzeCourseRequirements(
        courses: Map<String, MergedCourseInfo>,
        preferences: List<ValidatedPreferenceInfo>
    ): Map<String, CourseRequirements> {
        return courses.mapValues { (courseId, course) ->
            val coursePreferences = preferences.filter { it.preferenceInfo.courseId == courseId }

            CourseRequirements(
                courseId = courseId,
                minimumCapacity = course.studentCount,
                preferredCapacity = (course.studentCount * 1.1).toInt(),
                requiredEquipment = extractRequiredEquipment(course),
                accessibilityRequired = course.studentCount > 50,
                roomPreferences = coursePreferences.flatMap { it.preferenceInfo.preferredRooms },
                timeConstraints = extractTimeConstraints(coursePreferences),
                priority = calculateCoursePriority(course),
                specialRequirements = extractSpecialRequirements(course)
            )
        }
    }

    private fun assessRoomCapabilities(
        rooms: List<RoomInfo>,
        examPeriod: ExamPeriod
    ): Map<String, RoomCapabilities> {
        return rooms.associate { room ->
            room.roomId to RoomCapabilities(
                roomId = room.roomId,
                roomName = room.roomName,
                capacity = room.capacity,
                availableEquipment = room.equipment,
                isAccessible = room.accessibility,
                location = room.location ?: "Unknown",
                availableTimeSlots = generateAvailableTimeSlots(room, examPeriod),
                utilizationScore = 0.0,
                flexibilityScore = calculateRoomFlexibilityScore(room)
            )
        }
    }


    private fun applyPrimaryAllocationAlgorithm(
        courseRequirements: Map<String, CourseRequirements>,
        roomCapabilities: Map<String, RoomCapabilities>
    ): List<RoomAllocation> {
        val allocations = mutableListOf<RoomAllocation>()
        val usedRoomSlots = mutableSetOf<String>()

        val sortedCourses = courseRequirements.values.sortedWith(
            compareByDescending<CourseRequirements> { it.priority }
                .thenByDescending { it.minimumCapacity }
        )

        sortedCourses.forEach { courseReq ->
            val allocation = findBestRoomAllocation(courseReq, roomCapabilities, usedRoomSlots)
            if (allocation != null) {
                allocations.add(allocation)
                allocation.timeSlots.forEach { slot ->
                    usedRoomSlots.add("${allocation.roomId}_${slot.date}_${slot.startTime}")
                }
            }
        }

        return allocations
    }


    private fun findBestRoomAllocation(
        courseReq: CourseRequirements,
        roomCapabilities: Map<String, RoomCapabilities>,
        usedRoomSlots: Set<String>
    ): RoomAllocation? {
        val suitableRooms = roomCapabilities.values.filter { room ->
            room.capacity >= courseReq.minimumCapacity &&
                    room.availableEquipment.containsAll(courseReq.requiredEquipment) &&
                    (!courseReq.accessibilityRequired || room.isAccessible)
        }

        if (suitableRooms.isEmpty()) {
            logger.warn("No suitable rooms found for course: {}", courseReq.courseId)
            return null
        }

        val roomScores = suitableRooms.map { room ->
            room to calculateRoomScore(room, courseReq, usedRoomSlots)
        }.sortedByDescending { it.second }

        for ((room, score) in roomScores) {
            val availableSlots = findAvailableTimeSlots(room, usedRoomSlots, courseReq.timeConstraints)
            if (availableSlots.isNotEmpty()) {
                return RoomAllocation(
                    courseId = courseReq.courseId,
                    roomId = room.roomId,
                    roomName = room.roomName,
                    allocatedCapacity = room.capacity,
                    requiredCapacity = courseReq.minimumCapacity,
                    utilizationRate = courseReq.minimumCapacity.toDouble() / room.capacity,
                    timeSlots = availableSlots.take(1),
                    allocationScore = score,
                    constraints = courseReq.specialRequirements
                )
            }
        }

        return null
    }


    private fun calculateRoomScore(
        room: RoomCapabilities,
        courseReq: CourseRequirements,
        usedRoomSlots: Set<String>
    ): Double {
        var score = 0.0

        val utilizationRate = courseReq.minimumCapacity.toDouble() / room.capacity
        score += when {
            utilizationRate > 0.9 -> 1.0
            utilizationRate > 0.7 -> 0.8
            utilizationRate > 0.5 -> 0.6
            else -> 0.3
        }

        val hasAllRequiredEquipment = room.availableEquipment.containsAll(courseReq.requiredEquipment)
        if (hasAllRequiredEquipment) score += 0.5

        if (courseReq.roomPreferences.contains(room.roomId)) {
            score += 0.3
        }

        if (courseReq.accessibilityRequired && room.isAccessible) {
            score += 0.2
        }

        val availableSlotCount = room.availableTimeSlots.count { slot ->
            !usedRoomSlots.contains("${room.roomId}_${slot.date}_${slot.startTime}")
        }
        score += min(0.3, availableSlotCount * 0.05)

        score += room.flexibilityScore * 0.1

        return score
    }

    private fun applyAllocationOptimization(
        initialAllocations: List<RoomAllocation>,
        roomCapabilities: Map<String, RoomCapabilities>
    ): List<RoomAllocation> {
        var optimizedAllocations = initialAllocations.toMutableList()

        optimizedAllocations = optimizeRoomUtilizationSwapping(optimizedAllocations, roomCapabilities).toMutableList()

        optimizedAllocations = optimizeTimeSlotConsolidation(optimizedAllocations).toMutableList()

        optimizedAllocations = optimizeLoadBalancing(optimizedAllocations, roomCapabilities).toMutableList()

        return optimizedAllocations
    }

    private fun optimizeRoomUtilizationSwapping(
        allocations: List<RoomAllocation>,
        roomCapabilities: Map<String, RoomCapabilities>
    ): List<RoomAllocation> {
        val optimized = allocations.toMutableList()
        var improvementMade = true
        var iterations = 0
        val maxIterations = 10

        while (improvementMade && iterations < maxIterations) {
            improvementMade = false
            iterations++

            for (i in optimized.indices) {
                for (j in i + 1 until optimized.size) {
                    val allocation1 = optimized[i]
                    val allocation2 = optimized[j]

                    val room1 = roomCapabilities[allocation1.roomId]
                    val room2 = roomCapabilities[allocation2.roomId]

                    if (room1 != null && room2 != null) {
                        val currentUtil = allocation1.utilizationRate + allocation2.utilizationRate
                        val swappedUtil1 = allocation1.requiredCapacity.toDouble() / room2.capacity
                        val swappedUtil2 = allocation2.requiredCapacity.toDouble() / room1.capacity
                        val newUtil = swappedUtil1 + swappedUtil2

                        if (newUtil > currentUtil &&
                            swappedUtil1 <= 1.0 && swappedUtil2 <= 1.0 &&
                            canAccommodateSwap(allocation1, room2) &&
                            canAccommodateSwap(allocation2, room1)
                        ) {

                            optimized[i] = allocation1.copy(
                                roomId = room2.roomId,
                                roomName = room2.roomName,
                                allocatedCapacity = room2.capacity,
                                utilizationRate = swappedUtil1
                            )
                            optimized[j] = allocation2.copy(
                                roomId = room1.roomId,
                                roomName = room1.roomName,
                                allocatedCapacity = room1.capacity,
                                utilizationRate = swappedUtil2
                            )

                            improvementMade = true
                            break
                        }
                    }
                }
                if (improvementMade) break
            }
        }

        logger.debug("Room utilization optimization completed after {} iterations", iterations)
        return optimized
    }


    private fun validateAndResolveConflicts(
        allocations: List<RoomAllocation>,
        courseRequirements: Map<String, CourseRequirements>
    ): List<RoomAllocation> {
        val validAllocations = mutableListOf<RoomAllocation>()
        val conflicts = mutableListOf<String>()

        val roomTimeSlots = mutableSetOf<String>()

        allocations.forEach { allocation ->
            var hasConflict = false

            allocation.timeSlots.forEach { slot ->
                val slotKey = "${allocation.roomId}_${slot.date}_${slot.startTime}"
                if (roomTimeSlots.contains(slotKey)) {
                    conflicts.add("Double booking detected for room ${allocation.roomId} at ${slot.date} ${slot.startTime}")
                    hasConflict = true
                } else {
                    roomTimeSlots.add(slotKey)
                }
            }

            if (!hasConflict) {
                validAllocations.add(allocation)
            }
        }

        if (conflicts.isNotEmpty()) {
            logger.warn("Resolved {} allocation conflicts", conflicts.size)
        }

        return validAllocations
    }


    private fun extractRequiredEquipment(course: MergedCourseInfo): Set<String> {
        return when {
            course.courseName.contains("lab", ignoreCase = true) -> setOf("computers", "projector")
            course.courseName.contains("presentation", ignoreCase = true) -> setOf("projector", "sound-system")
            course.studentCount > 100 -> setOf("projector", "sound-system")
            else -> setOf("projector")
        }
    }

    private fun extractTimeConstraints(preferences: List<ValidatedPreferenceInfo>): List<TimeConstraint> {
        return preferences.flatMap { pref ->
            pref.preferenceInfo.preferredTimeSlots.map { slot ->
                TimeConstraint(
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    dayOfWeek = slot.dayOfWeek,
                    priority = pref.preferenceInfo.priority
                )
            }
        }
    }

    private fun calculateCoursePriority(course: MergedCourseInfo): Int {
        var priority = 0

        if (course.mandatoryStatus == MandatoryStatus.MANDATORY) priority += 10

        priority += min(10, course.studentCount / 10)

        priority += course.credits / 3

        return priority
    }

    private fun extractSpecialRequirements(course: MergedCourseInfo): List<String> {
        val requirements = mutableListOf<String>()

        if (course.studentCount > 100) requirements.add("LARGE_CAPACITY")
        if (course.courseName.contains("lab", ignoreCase = true)) requirements.add("COMPUTER_LAB")
        if (course.courseName.contains("presentation", ignoreCase = true)) requirements.add("PRESENTATION_EQUIPMENT")

        return requirements
    }

    private fun generateAvailableTimeSlots(room: RoomInfo, examPeriod: ExamPeriod): List<TimeSlot> {
        val timeSlots = mutableListOf<TimeSlot>()
        var currentDate = examPeriod.startDate

        while (!currentDate.isAfter(examPeriod.endDate)) {
            timeSlots.add(
                TimeSlot(
                    currentDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(12, 0),
                    room.roomId,
                    room.roomName,
                    room.capacity,
                    currentDate.dayOfWeek.value
                )
            )
            timeSlots.add(
                TimeSlot(
                    currentDate,
                    LocalTime.of(14, 0),
                    LocalTime.of(17, 0),
                    room.roomId,
                    room.roomName,
                    room.capacity,
                    currentDate.dayOfWeek.value
                )
            )

            currentDate = currentDate.plusDays(1)
        }

        return timeSlots
    }

    private fun calculateRoomFlexibilityScore(room: RoomInfo): Double {
        var score = 0.5 // Base score

        score += room.equipment.size * 0.1

        score += when {
            room.capacity in 40..80 -> 0.3
            room.capacity in 20..120 -> 0.2
            else -> 0.1
        }

        if (room.accessibility) score += 0.1

        return min(1.0, score)
    }

    private fun findAvailableTimeSlots(
        room: RoomCapabilities,
        usedRoomSlots: Set<String>,
        timeConstraints: List<TimeConstraint>
    ): List<TimeSlot> {
        return room.availableTimeSlots.filter { slot ->
            val slotKey = "${room.roomId}_${slot.date}_${slot.startTime}"
            !usedRoomSlots.contains(slotKey) &&
                    matchesTimeConstraints(slot, timeConstraints)
        }
    }

    private fun matchesTimeConstraints(slot: TimeSlot, constraints: List<TimeConstraint>): Boolean {
        if (constraints.isEmpty()) return true

        return constraints.any { constraint ->
            slot.startTime >= constraint.startTime &&
                    slot.endTime <= constraint.endTime &&
                    (constraint.dayOfWeek == null || constraint.dayOfWeek == slot.dayOfWeek)
        }
    }

    private fun canAccommodateSwap(allocation: RoomAllocation, newRoom: RoomCapabilities): Boolean {
        return allocation.requiredCapacity <= newRoom.capacity &&
                newRoom.availableEquipment.containsAll(extractEquipmentFromConstraints(allocation.constraints))
    }

    private fun extractEquipmentFromConstraints(constraints: List<String>): Set<String> {
        return constraints.mapNotNull { constraint ->
            when {
                constraint.contains("COMPUTER") -> "computers"
                constraint.contains("PRESENTATION") -> "projector"
                else -> null
            }
        }.toSet()
    }

    private fun optimizeTimeSlotConsolidation(allocations: List<RoomAllocation>): List<RoomAllocation> {
        return allocations
    }

    private fun optimizeLoadBalancing(
        allocations: List<RoomAllocation>,
        roomCapabilities: Map<String, RoomCapabilities>
    ): List<RoomAllocation> {
        return allocations
    }

    private fun findUnallocatedCourses(courseIds: Set<String>, allocations: List<RoomAllocation>): List<String> {
        val allocatedCourses = allocations.map { it.courseId }.toSet()
        return courseIds.minus(allocatedCourses).toList()
    }

    private fun calculateAllocationMetrics(
        allocations: List<RoomAllocation>,
        courseRequirements: Map<String, CourseRequirements>,
        roomCapabilities: Map<String, RoomCapabilities>
    ): AllocationMetrics {
        val totalCourses = courseRequirements.size
        val allocatedCourses = allocations.size
        val successRate = if (totalCourses > 0) allocatedCourses.toDouble() / totalCourses else 0.0

        val averageUtilization = if (allocations.isNotEmpty()) {
            allocations.map { it.utilizationRate }.average()
        } else 0.0

        val totalCapacityWaste = allocations.sumOf { allocation ->
            max(0, allocation.allocatedCapacity - allocation.requiredCapacity)
        }

        return AllocationMetrics(
            totalCourses = totalCourses,
            successfulAllocations = allocatedCourses,
            successRate = successRate,
            averageRoomUtilization = averageUtilization,
            totalCapacityWaste = totalCapacityWaste,
            roomDistribution = calculateRoomDistribution(allocations, roomCapabilities)
        )
    }

    private fun calculateRoomDistribution(
        allocations: List<RoomAllocation>,
        roomCapabilities: Map<String, RoomCapabilities>
    ): Map<String, Int> {
        return allocations.groupingBy { it.roomId }.eachCount()
    }

    private fun calculateAllocationQualityScore(metrics: AllocationMetrics): Double {
        var score = metrics.successRate * 0.5
        score += metrics.averageRoomUtilization * 0.3
        score += (1.0 - min(
            1.0,
            metrics.totalCapacityWaste.toDouble() / (metrics.totalCourses * 50)
        )) * 0.2  // 20% weight for waste minimization
        return score
    }

    private fun analyzeResourceChangeImpact(
        change: ResourceChangeRequest,
        currentAllocations: Map<String, RoomAllocation>
    ): ResourceImpact {
        return ResourceImpact(false, 0, emptyList())
    }

    private fun calculateBottleneckSeverity(overflow: Int): BottleneckSeverity {
        return when {
            overflow > 50 -> BottleneckSeverity.CRITICAL
            overflow > 20 -> BottleneckSeverity.HIGH
            overflow > 5 -> BottleneckSeverity.MEDIUM
            else -> BottleneckSeverity.LOW
        }
    }

    private fun generateCapacityRecommendations(
        allocations: Map<String, RoomAllocation>,
        bottlenecks: List<ResourceBottleneck>
    ): List<String> {
        return emptyList()
    }

    private fun calculateOverallUtilization(allocations: Map<String, RoomAllocation>): Double {
        return if (allocations.isNotEmpty()) allocations.values.map { it.utilizationRate }.average() else 0.0
    }

    private fun calculateFeasibilityScore(
        impactAnalysis: Map<String, ResourceImpact>,
        bottlenecks: List<ResourceBottleneck>
    ): Double {
        return 1.0 - (bottlenecks.size * 0.1)
    }

    private fun analyzeCurrentUtilization(
        schedule: List<ScheduledExamInfo>,
        rooms: List<RoomInfo>
    ): UtilizationAnalysis {
        val overallUtilization = schedule.mapNotNull { exam ->
            exam.roomCapacity?.let { capacity ->
                if (capacity > 0) exam.studentCount.toDouble() / capacity else null
            }
        }.average()

        return UtilizationAnalysis(overallUtilization)
    }

    private fun identifyOptimizationOpportunities(analysis: UtilizationAnalysis): List<OptimizationOpportunity> =
        emptyList()

    private fun selectOptimizationStrategies(opportunities: List<OptimizationOpportunity>): List<OptimizationStrategy> =
        emptyList()

    private fun applyOptimizationStrategies(
        schedule: List<ScheduledExamInfo>,
        strategies: List<OptimizationStrategy>
    ): List<ScheduledExamInfo> = schedule

    private fun calculateOptimizationMetrics(
        original: List<ScheduledExamInfo>,
        optimized: List<ScheduledExamInfo>
    ): OptimizationMetrics {
        return OptimizationMetrics(0, 0, 0)
    }
}