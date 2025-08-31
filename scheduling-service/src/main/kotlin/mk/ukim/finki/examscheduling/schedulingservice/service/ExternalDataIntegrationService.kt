package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.max
import kotlin.math.min

@Service
@Transactional
class ExternalDataIntegrationService(
    private val externalIntegrationClient: ExternalIntegrationClient,
    private val preferenceManagementClient: PreferenceManagementClient,
    private val dataValidationService: ExternalDataValidationService,
) {

    private val logger = LoggerFactory.getLogger(ExternalDataIntegrationService::class.java)

    /**
     * Comprehensive external data fetching with anti-corruption layers.
     */
    fun fetchSchedulingData(
        examSessionPeriodId: String,
        academicYear: String,
        examSession: String
    ): CompletableFuture<IntegratedSchedulingData> {
        logger.info("Fetching integrated scheduling data for session: {}", examSessionPeriodId)

        val dataFetchStart = Instant.now()

        val accreditationDataFuture = fetchAccreditationDataWithACL(academicYear, examSession)
        val enrollmentDataFuture = fetchEnrollmentDataWithACL(academicYear, examSession)
        val preferencesDataFuture = fetchPreferencesDataWithACL(academicYear, examSession)
        val resourceDataFuture = fetchResourceAvailabilityData(examSessionPeriodId)

        return CompletableFuture.allOf(
            accreditationDataFuture, enrollmentDataFuture, preferencesDataFuture, resourceDataFuture
        ).thenCompose { _ ->
            try {
                val accreditationData = accreditationDataFuture!!.get()
                val enrollmentData = enrollmentDataFuture.get()
                val preferencesData = preferencesDataFuture.get()
                val resourceData = resourceDataFuture.get()

                integrateAndValidateData(
                    examSessionPeriodId,
                    accreditationData,
                    enrollmentData,
                    preferencesData,
                    resourceData,
                    dataFetchStart
                )
            } catch (e: Exception) {
                logger.error("Failed to fetch external data for session: {}", examSessionPeriodId, e)
                CompletableFuture.completedFuture(generateFallbackData(examSessionPeriodId, e))
            }
        }
    }

    private fun fetchAccreditationDataWithACL(academicYear: String, examSession: String): CompletableFuture<AccreditationIntegrationResult>? {
        return CompletableFuture.completedFuture(null)
    }


    private fun fetchEnrollmentDataWithACL(
        academicYear: String,
        examSession: String
    ): CompletableFuture<EnrollmentIntegrationResult> {
        logger.debug("Fetching enrollment data with ACL for {}/{}", academicYear, examSession)

        return CompletableFuture.completedFuture(null)
    }

    private fun fetchPreferencesDataWithACL(
        academicYear: String,
        examSession: String
    ): CompletableFuture<PreferencesIntegrationResult> {
        logger.debug("Fetching preferences data for {}/{}", academicYear, examSession)

        return preferenceManagementClient.getPreferencesBySession(academicYear, examSession)
            .thenCompose { rawData ->
                CompletableFuture.supplyAsync {
                    processPreferencesData(rawData, academicYear, examSession)
                }
            }
    }

    private fun fetchResourceAvailabilityData(
        examSessionPeriodId: String
    ): CompletableFuture<ResourceAvailabilityResult> {
        logger.debug("Fetching resource availability for session: {}", examSessionPeriodId)

        return CompletableFuture.supplyAsync {
            generateResourceAvailabilityData(examSessionPeriodId)
        }
    }

    private fun integrateAndValidateData(
        examSessionPeriodId: String,
        accreditationData: AccreditationIntegrationResult,
        enrollmentData: EnrollmentIntegrationResult,
        preferencesData: PreferencesIntegrationResult,
        resourceData: ResourceAvailabilityResult,
        fetchStartTime: Instant
    ): CompletableFuture<IntegratedSchedulingData> {
        return CompletableFuture.supplyAsync {
            logger.info("Integrating data from {} sources for session: {}", 4, examSessionPeriodId)


            val mergedCourseData = mergeCourseData(accreditationData.courses, enrollmentData.enrollments)
            val validatedPreferences = validateAndFilterPreferences(preferencesData.preferences, mergedCourseData)
            val optimizedResources = optimizeResourceAllocation(resourceData.resources, mergedCourseData)

            val dataQualityMetrics = calculateDataQualityMetrics(
                accreditationData, enrollmentData, preferencesData, resourceData
            )

            val fetchDuration = Instant.now().toEpochMilli() - fetchStartTime.toEpochMilli()

            IntegratedSchedulingData(
                examSessionPeriodId = examSessionPeriodId,
                courses = mergedCourseData,
                preferences = validatedPreferences,
                resources = optimizedResources,
                dataQuality = dataQualityMetrics,
                integrationMetadata = DataIntegrationMetadata(
                    fetchDuration = fetchDuration,
                    sourceCount = 4,
                    integratedAt = Instant.now()
                )
            )
        }
    }


    private fun processAccreditationData(
        rawData: Map<String, Any>,
        academicYear: String,
        examSession: String
    ): AccreditationIntegrationResult {
        logger.debug("Processing accreditation data through ACL")

        val courses = mutableMapOf<String, CourseAccreditationInfo>()
        val issues = mutableListOf<DataIntegrationIssue>()
        val sourceMetadata = extractSourceMetadata(rawData)

        try {
            val coursesData = rawData["courses"] as? List<Map<String, Any>> ?: emptyList()

            coursesData.forEach { courseData ->
                try {
                    val courseInfo = mapAccreditationCourse(courseData)
                    if (dataValidationService.validateCourseData(courseInfo)) {
                        courses[courseInfo.courseId] = courseInfo
                    } else {
                        issues.add(
                            DataIntegrationIssue(
                                type = "VALIDATION_FAILED",
                                source = "accreditation",
                                entityId = courseInfo.courseId,
                                description = "Course data failed validation",
                                severity = IssueSeverity.WARNING
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to process course data: {}", courseData, e)
                    issues.add(
                        DataIntegrationIssue(
                            type = "MAPPING_ERROR",
                            source = "accreditation",
                            entityId = courseData["courseId"]?.toString() ?: "unknown",
                            description = "Failed to map course data: ${e.message}",
                            severity = IssueSeverity.ERROR
                        )
                    )
                }
            }

        } catch (e: Exception) {
            logger.error("Critical error processing accreditation data", e)
            issues.add(
                DataIntegrationIssue(
                    type = "CRITICAL_ERROR",
                    source = "accreditation",
                    entityId = "ALL",
                    description = "Critical processing error: ${e.message}",
                    severity = IssueSeverity.CRITICAL
                )
            )
        }

        return AccreditationIntegrationResult(
            courses = courses,
            issues = issues,
            sourceMetadata = sourceMetadata,
            processedAt = Instant.now()
        )
    }


    private fun processEnrollmentData(
        rawData: Map<String, Any>,
        academicYear: String,
        examSession: String
    ): EnrollmentIntegrationResult {
        logger.debug("Processing enrollment data through ACL")

        val enrollments = mutableMapOf<String, CourseEnrollmentInfo>()
        val issues = mutableListOf<DataIntegrationIssue>()
        val sourceMetadata = extractSourceMetadata(rawData)

        try {
            val enrollmentData = rawData["enrollmentData"] as? Map<String, Any>
                ?: rawData["courses"] as? List<Map<String, Any>>

            when (enrollmentData) {
                is Map<*, *> -> processEnrollmentMap(enrollmentData as Map<String, Any>, enrollments, issues)
                is List<*> -> processEnrollmentList(enrollmentData as List<Map<String, Any>>, enrollments, issues)
                else -> {
                    issues.add(
                        DataIntegrationIssue(
                            type = "INVALID_FORMAT",
                            source = "enrollment",
                            entityId = "ALL",
                            description = "Unrecognized enrollment data format",
                            severity = IssueSeverity.ERROR
                        )
                    )
                }
            }

        } catch (e: Exception) {
            logger.error("Critical error processing enrollment data", e)
            issues.add(
                DataIntegrationIssue(
                    type = "CRITICAL_ERROR",
                    source = "enrollment",
                    entityId = "ALL",
                    description = "Critical processing error: ${e.message}",
                    severity = IssueSeverity.CRITICAL
                )
            )
        }

        return EnrollmentIntegrationResult(
            enrollments = enrollments,
            issues = issues,
            sourceMetadata = sourceMetadata,
            processedAt = Instant.now()
        )
    }

    private fun processPreferencesData(
        rawData: Map<String, Any>,
        academicYear: String,
        examSession: String
    ): PreferencesIntegrationResult {
        logger.debug("Processing preferences data")

        val preferences = mutableListOf<ProfessorPreferenceInfo>()
        val issues = mutableListOf<DataIntegrationIssue>()

        try {
            val preferencesData = rawData["preferences"] as? List<Map<String, Any>> ?: emptyList()

            preferencesData.forEach { prefData ->
                try {
                    val preference = mapPreferenceData(prefData)
                    if (dataValidationService.validatePreferenceData(preference)) {
                        preferences.add(preference)
                    } else {
                        issues.add(
                            DataIntegrationIssue(
                                type = "VALIDATION_FAILED",
                                source = "preferences",
                                entityId = preference.preferenceId,
                                description = "Preference data failed validation",
                                severity = IssueSeverity.WARNING
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to process preference data: {}", prefData, e)
                    issues.add(
                        DataIntegrationIssue(
                            type = "MAPPING_ERROR",
                            source = "preferences",
                            entityId = prefData["preferenceId"]?.toString() ?: "unknown",
                            description = "Failed to map preference data: ${e.message}",
                            severity = IssueSeverity.ERROR
                        )
                    )
                }
            }

        } catch (e: Exception) {
            logger.error("Critical error processing preferences data", e)
            issues.add(
                DataIntegrationIssue(
                    type = "CRITICAL_ERROR",
                    source = "preferences",
                    entityId = "ALL",
                    description = "Critical processing error: ${e.message}",
                    severity = IssueSeverity.CRITICAL
                )
            )
        }

        return PreferencesIntegrationResult(
            preferences = preferences,
            issues = issues,
            sourceMetadata = mapOf("totalPreferences" to preferences.size),
            processedAt = Instant.now()
        )
    }


    private fun mapAccreditationCourse(courseData: Map<String, Any>): CourseAccreditationInfo {
        return CourseAccreditationInfo(
            courseId = courseData["courseId"] as? String ?: throw IllegalArgumentException("Missing courseId"),
            courseName = courseData["courseName"] as? String ?: throw IllegalArgumentException("Missing courseName"),
            mandatoryStatus = when (courseData["mandatory"] as? Boolean) {
                true -> MandatoryStatus.MANDATORY
                false -> MandatoryStatus.ELECTIVE
                null -> MandatoryStatus.MANDATORY
            },
            credits = courseData["credits"] as? Int ?: 6,
            professorIds = (courseData["professors"] as? List<String>)?.toSet() ?: emptySet(),
            prerequisites = (courseData["prerequisites"] as? List<String>)?.toSet() ?: emptySet(),
            accreditationDetails = courseData.filterKeys {
                it !in setOf("courseId", "courseName", "mandatory", "credits", "professors", "prerequisites")
            }
        )
    }

    private fun processEnrollmentMap(
        enrollmentData: Map<String, Any>,
        enrollments: MutableMap<String, CourseEnrollmentInfo>,
        issues: MutableList<DataIntegrationIssue>
    ) {
        enrollmentData.forEach { (courseId, enrollmentInfo) ->
            try {
                val info = when (enrollmentInfo) {
                    is Map<*, *> -> {
                        val infoMap = enrollmentInfo as Map<String, Any>
                        CourseEnrollmentInfo(
                            courseId = courseId,
                            studentCount = infoMap["studentCount"] as? Int ?: infoMap["count"] as? Int ?: 0,
                            enrollmentDetails = infoMap.filterKeys { it != "studentCount" && it != "count" }
                        )
                    }

                    is Number -> {
                        CourseEnrollmentInfo(
                            courseId = courseId,
                            studentCount = enrollmentInfo.toInt(),
                            enrollmentDetails = emptyMap()
                        )
                    }

                    else -> throw IllegalArgumentException("Invalid enrollment info format for course $courseId")
                }

                enrollments[courseId] = info
            } catch (e: Exception) {
                issues.add(
                    DataIntegrationIssue(
                        type = "MAPPING_ERROR",
                        source = "enrollment",
                        entityId = courseId,
                        description = "Failed to process enrollment: ${e.message}",
                        severity = IssueSeverity.WARNING
                    )
                )
            }
        }
    }

    private fun processEnrollmentList(
        enrollmentData: List<Map<String, Any>>,
        enrollments: MutableMap<String, CourseEnrollmentInfo>,
        issues: MutableList<DataIntegrationIssue>
    ) {
        enrollmentData.forEach { courseData ->
            try {
                val courseId = courseData["courseId"] as? String ?: throw IllegalArgumentException("Missing courseId")
                val studentCount = courseData["enrolledStudents"] as? Int
                    ?: courseData["studentCount"] as? Int
                    ?: 0

                enrollments[courseId] = CourseEnrollmentInfo(
                    courseId = courseId,
                    studentCount = studentCount,
                    enrollmentDetails = courseData.filterKeys { it != "courseId" && it != "enrolledStudents" && it != "studentCount" }
                )
            } catch (e: Exception) {
                issues.add(
                    DataIntegrationIssue(
                        type = "MAPPING_ERROR",
                        source = "enrollment",
                        entityId = courseData["courseId"]?.toString() ?: "unknown",
                        description = "Failed to process enrollment: ${e.message}",
                        severity = IssueSeverity.WARNING
                    )
                )
            }
        }
    }

    private fun mapPreferenceData(prefData: Map<String, Any>): ProfessorPreferenceInfo {
        return ProfessorPreferenceInfo(
            preferenceId = prefData["preferenceId"] as? String ?: UUID.randomUUID().toString(),
            professorId = prefData["professorId"] as? String ?: throw IllegalArgumentException("Missing professorId"),
            courseId = prefData["courseId"] as? String ?: throw IllegalArgumentException("Missing courseId"),
            preferredDates = parsePreferredDates(prefData["preferredDates"]),
            preferredTimeSlots = parsePreferredTimeSlots(prefData["preferredTimeSlots"]),
            preferredRooms = (prefData["preferredRooms"] as? List<String>) ?: emptyList(),
            unavailableDates = parsePreferredDates(prefData["unavailableDates"]),
            unavailableTimeSlots = parsePreferredTimeSlots(prefData["unavailableTimeSlots"]),
            specialRequirements = prefData["specialRequirements"] as? String,
            priority = prefData["priority"] as? Int ?: 3
        )
    }


    private fun mergeCourseData(
        accreditationCourses: Map<String, CourseAccreditationInfo>,
        enrollments: Map<String, CourseEnrollmentInfo>
    ): Map<String, MergedCourseInfo> {
        val merged = mutableMapOf<String, MergedCourseInfo>()

        accreditationCourses.forEach { (courseId, accreditation) ->
            val enrollment = enrollments[courseId] ?: CourseEnrollmentInfo(courseId, 0)

            merged[courseId] = MergedCourseInfo(
                courseId = courseId,
                courseName = accreditation.courseName,
                studentCount = enrollment.studentCount,
                professorIds = accreditation.professorIds,
                mandatoryStatus = accreditation.mandatoryStatus,
                credits = accreditation.credits,
                prerequisites = accreditation.prerequisites,
                estimatedDuration = calculateExamDuration(accreditation.credits),
                dataQuality = calculateCourseDataQuality(accreditation, enrollment)
            )
        }

        enrollments.keys.minus(accreditationCourses.keys).forEach { orphanedCourseId ->
            val enrollment = enrollments[orphanedCourseId]!!
            logger.warn("Found enrollment without accreditation data: {}", orphanedCourseId)

            merged[orphanedCourseId] = MergedCourseInfo(
                courseId = orphanedCourseId,
                courseName = "Unknown Course ($orphanedCourseId)",
                studentCount = enrollment.studentCount,
                professorIds = emptySet(),
                mandatoryStatus = MandatoryStatus.ELECTIVE,
                credits = 6,
                prerequisites = emptySet(),
                estimatedDuration = 120,
                dataQuality = DataQuality.LOW
            )
        }

        return merged
    }

    private fun validateAndFilterPreferences(
        preferences: List<ProfessorPreferenceInfo>,
        courseData: Map<String, MergedCourseInfo>
    ): List<ValidatedPreferenceInfo> {
        return preferences.mapNotNull { preference ->
            val course = courseData[preference.courseId]
            if (course != null) {
                ValidatedPreferenceInfo(
                    preferenceInfo = preference,
                    courseExists = true,
                    professorAuthorized = course.professorIds.isEmpty() || course.professorIds.contains(preference.professorId),
                    validationScore = calculatePreferenceValidationScore(preference, course)
                )
            } else {
                logger.warn("Preference for non-existent course: {}", preference.courseId)
                ValidatedPreferenceInfo(
                    preferenceInfo = preference,
                    courseExists = false,
                    professorAuthorized = false,
                    validationScore = 0.0
                )
            }
        }.filter { it.courseExists && it.professorAuthorized }
    }

    private fun optimizeResourceAllocation(
        resources: List<RoomInfo>,
        courseData: Map<String, MergedCourseInfo>
    ): List<OptimizedRoomInfo> {
        return resources.map { room ->
            val suitableCourses = courseData.values.filter { it.studentCount <= room.capacity }
            val utilizationPotential = calculateRoomUtilizationPotential(room, suitableCourses)

            OptimizedRoomInfo(
                roomInfo = room,
                suitableCourseCount = suitableCourses.size,
                utilizationPotential = utilizationPotential,
                priority = calculateRoomPriority(room, utilizationPotential)
            )
        }.sortedByDescending { it.priority }
    }


    private fun parsePreferredDates(dates: Any?): List<java.time.LocalDate> {
        return when (dates) {
            is List<*> -> dates.mapNotNull { dateStr ->
                try {
                    java.time.LocalDate.parse(dateStr.toString())
                } catch (e: Exception) {
                    logger.warn("Failed to parse date: {}", dateStr)
                    null
                }
            }

            else -> emptyList()
        }
    }

    private fun parsePreferredTimeSlots(timeSlots: Any?): List<TimeSlotPreference> {
        return when (timeSlots) {
            is List<*> -> timeSlots.mapNotNull { slot ->
                try {
                    val slotMap = slot as Map<String, Any>
                    TimeSlotPreference(
                        startTime = java.time.LocalTime.parse(slotMap["startTime"] as String),
                        endTime = java.time.LocalTime.parse(slotMap["endTime"] as String),
                        dayOfWeek = slotMap["dayOfWeek"] as? Int
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to parse time slot: {}", slot)
                    null
                }
            }

            else -> emptyList()
        }
    }

    private fun calculateExamDuration(credits: Int): Int {
        return when (credits) {
            in 1..3 -> 90
            in 4..6 -> 120
            in 7..9 -> 180
            else -> 120
        }
    }

    private fun calculateCourseDataQuality(
        accreditation: CourseAccreditationInfo,
        enrollment: CourseEnrollmentInfo
    ): DataQuality {
        var score = 1.0

        if (accreditation.professorIds.isEmpty()) score -= 0.3
        if (enrollment.studentCount == 0) score -= 0.2
        if (accreditation.courseName.contains("unknown", ignoreCase = true)) score -= 0.4

        return when {
            score >= 0.8 -> DataQuality.HIGH
            score >= 0.5 -> DataQuality.MEDIUM
            else -> DataQuality.LOW
        }
    }

    private fun calculatePreferenceValidationScore(
        preference: ProfessorPreferenceInfo,
        course: MergedCourseInfo
    ): Double {
        var score = 1.0

        if (preference.preferredDates.isEmpty() && preference.preferredTimeSlots.isEmpty()) {
            score -= 0.5
        }

        if (preference.unavailableDates.size > 10) {
            score -= 0.2
        }

        return max(0.0, score)
    }

    private fun calculateRoomUtilizationPotential(
        room: RoomInfo,
        suitableCourses: List<MergedCourseInfo>
    ): Double {
        if (suitableCourses.isEmpty()) return 0.0

        val totalStudents = suitableCourses.sumOf { it.studentCount }
        val averageUtilization = totalStudents.toDouble() / (suitableCourses.size * room.capacity)

        return max(0.0, min(1.0, averageUtilization))
    }

    private fun calculateRoomPriority(room: RoomInfo, utilizationPotential: Double): Double {
        var priority = utilizationPotential * 0.6

        if (room.accessibility) priority += 0.2

        priority += room.equipment.size * 0.05

        val capacityScore = when {
            room.capacity in 30..80 -> 0.2
            room.capacity in 80..150 -> 0.15
            room.capacity > 150 -> 0.1
            else -> 0.05
        }
        priority += capacityScore

        return priority
    }

    private fun calculateDataQualityMetrics(
        accreditationData: AccreditationIntegrationResult,
        enrollmentData: EnrollmentIntegrationResult,
        preferencesData: PreferencesIntegrationResult,
        resourceData: ResourceAvailabilityResult): DataQualityMetrics {
        val totalIssues = accreditationData.issues.size + enrollmentData.issues.size + preferencesData.issues.size
        val criticalIssues = listOf(accreditationData.issues, enrollmentData.issues, preferencesData.issues)
            .flatten()
            .count { it.severity == IssueSeverity.CRITICAL }

        val completenessScore = calculateCompletenessScore(accreditationData, enrollmentData, preferencesData)
        val freshnessScore = calculateFreshnessScore(accreditationData, enrollmentData, preferencesData)

        return DataQualityMetrics(
            overallScore = (completenessScore  + freshnessScore) / 3.0,
            completenessScore = completenessScore,
            freshnessScore = freshnessScore,
            totalIssues = totalIssues,
            criticalIssues = criticalIssues,
            dataSourceHealth = mapOf(
                "accreditation" to calculateSourceHealth(accreditationData.issues),
                "enrollment" to calculateSourceHealth(enrollmentData.issues),
                "preferences" to calculateSourceHealth(preferencesData.issues)
            )
        )
    }

    private fun calculateCompletenessScore(
        accreditationData: AccreditationIntegrationResult,
        enrollmentData: EnrollmentIntegrationResult,
        preferencesData: PreferencesIntegrationResult
    ): Double {
        val accreditationCompleteness = if (accreditationData.courses.isNotEmpty()) 1.0 else 0.0
        val enrollmentCompleteness = if (enrollmentData.enrollments.isNotEmpty()) 1.0 else 0.0
        val preferencesCompleteness = if (preferencesData.preferences.isNotEmpty()) 0.8 else 0.0

        return (accreditationCompleteness + enrollmentCompleteness + preferencesCompleteness) / 3.0
    }

    private fun calculateFreshnessScore(
        accreditationData: AccreditationIntegrationResult,
        enrollmentData: EnrollmentIntegrationResult,
        preferencesData: PreferencesIntegrationResult
    ): Double {
        val now = Instant.now()
        val maxAge = 24 * 60 * 60 * 1000L

        val ages = listOf(
            now.toEpochMilli() - accreditationData.processedAt.toEpochMilli(),
            now.toEpochMilli() - enrollmentData.processedAt.toEpochMilli(),
            now.toEpochMilli() - preferencesData.processedAt.toEpochMilli()
        )

        val avgAge = ages.average()
        return max(0.0, 1.0 - (avgAge / maxAge))
    }

    private fun calculateSourceHealth(issues: List<DataIntegrationIssue>): Double {
        if (issues.isEmpty()) return 1.0

        val criticalCount = issues.count { it.severity == IssueSeverity.CRITICAL }
        val errorCount = issues.count { it.severity == IssueSeverity.ERROR }
        val warningCount = issues.count { it.severity == IssueSeverity.WARNING }

        val healthPenalty = criticalCount * 0.5 + errorCount * 0.3 + warningCount * 0.1
        return max(0.0, 1.0 - healthPenalty)
    }

    private fun extractSourceMetadata(rawData: Map<String, Any>): Map<String, Any> {
        return mapOf(
            // "recordCount" to (rawData["courses"] as? List<*>)?.size ?: 0,
            "lastUpdated" to (rawData["lastUpdated"] ?: rawData["timestamp"] ?: Instant.now()),
            "version" to (rawData["version"] ?: "unknown"),
            "source" to (rawData["source"] ?: "external-service")
        )
    }

    private fun generateResourceAvailabilityData(examSessionPeriodId: String): ResourceAvailabilityResult {
        // Generate default resource availability - in production this would call actual resource service
        val defaultRooms = listOf(
            RoomInfo(
                "A1-001",
                "Lecture Hall A1-001",
                100,
                setOf("projector", "whiteboard", "sound-system"),
                "Building A1",
                true
            ),
            RoomInfo("A1-002", "Classroom A1-002", 50, setOf("projector", "whiteboard"), "Building A1", true),
            RoomInfo(
                "A2-101",
                "Lecture Hall A2-101",
                150,
                setOf("projector", "whiteboard", "sound-system", "recording"),
                "Building A2",
                true
            ),
            RoomInfo("A2-102", "Classroom A2-102", 40, setOf("projector"), "Building A2", false),
            RoomInfo("B1-001", "Lab B1-001", 30, setOf("computers", "projector"), "Building B1", true),
            RoomInfo("B1-002", "Seminar Room B1-002", 25, setOf("whiteboard"), "Building B1", true)
        )

        return ResourceAvailabilityResult(
            resources = defaultRooms,
            availabilityPeriod = examSessionPeriodId,
            generatedAt = Instant.now()
        )
    }

    private fun generateFallbackData(examSessionPeriodId: String, exception: Exception): IntegratedSchedulingData {
        logger.error("Generating fallback integrated data due to: {}", exception.message)

        return IntegratedSchedulingData(
            examSessionPeriodId = examSessionPeriodId,
            courses = emptyMap(),
            preferences = emptyList(),
            resources = generateResourceAvailabilityData(examSessionPeriodId).resources.map { room ->
                OptimizedRoomInfo(room, 0, 0.0, 0.0)
            },
            dataQuality = DataQualityMetrics(
                overallScore = 0.1,
                completenessScore = 0.0,
                freshnessScore = 0.0,
                totalIssues = 1,
                criticalIssues = 1,
                dataSourceHealth = mapOf(
                    "accreditation" to 0.0,
                    "enrollment" to 0.0,
                    "preferences" to 0.0
                )
            ),
            integrationMetadata = DataIntegrationMetadata(
                fetchDuration = 0L,
                sourceCount = 0,
                integratedAt = Instant.now()
            )
        )
    }
}