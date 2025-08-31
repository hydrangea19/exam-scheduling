package mk.ukim.finki.examscheduling.schedulingservice.service

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictSeverity
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictStatus
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.ConflictType
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.schedulingservice.repository.ScheduleConflictRepository
import mk.ukim.finki.examscheduling.schedulingservice.repository.ScheduledExamRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalTime
import java.util.*

@Service
@Transactional(readOnly = true)
class ConflictAnalysisService(
    private val scheduleConflictRepository: ScheduleConflictRepository
) {

    private val logger = LoggerFactory.getLogger(ConflictAnalysisService::class.java)

    fun analyzeScheduleConflicts(
        scheduleId: UUID,
        scheduledExams: List<ScheduledExam>,
        enrollmentData: Map<String, CourseEnrollmentInfo>
    ): ConflictAnalysisResult {
        logger.info("Analyzing conflicts for schedule: {} with {} exams", scheduleId, scheduledExams.size)

        val timeConflicts = detectTimeConflicts(scheduledExams, enrollmentData)
        val spaceConflicts = detectSpaceConflicts(scheduledExams)
        val professorConflicts = detectProfessorConflicts(scheduledExams)

        val totalConflicts = timeConflicts.size + spaceConflicts.size + professorConflicts.size
        val criticalViolations = countCriticalViolations(timeConflicts, spaceConflicts, professorConflicts)

        storeDetectedConflicts(scheduleId, timeConflicts, spaceConflicts, professorConflicts)

        logger.info("Conflict analysis completed: {} total conflicts, {} critical violations",
            totalConflicts, criticalViolations)

        return ConflictAnalysisResult(
            totalExamSlots = scheduledExams.size,
            totalConflicts = totalConflicts,
            criticalViolations = criticalViolations,
            timeConflicts = timeConflicts,
            spaceConflicts = spaceConflicts,
            professorConflicts = professorConflicts
        )
    }

    /**
     * Detects time-based conflicts between exams and student overlaps.
     */
    private fun detectTimeConflicts(
        scheduledExams: List<ScheduledExam>,
        enrollmentData: Map<String, CourseEnrollmentInfo>
    ): List<TimeConflict> {
        val conflicts = mutableListOf<TimeConflict>()

        val examsByDate = scheduledExams.groupBy { it.examDate }

        examsByDate.forEach { (date, examsOnDate) ->
            for (i in examsOnDate.indices) {
                for (j in i + 1 until examsOnDate.size) {
                    val exam1 = examsOnDate[i]
                    val exam2 = examsOnDate[j]

                    if (timeSlotsOverlap(exam1.startTime, exam1.endTime, exam2.startTime, exam2.endTime)) {
                        val affectedStudents = estimateStudentOverlap(exam1, exam2, enrollmentData)

                        if (affectedStudents > 0) {
                            val severity = when {
                                affectedStudents > 50 -> ConflictSeverity.CRITICAL
                                affectedStudents > 20 -> ConflictSeverity.HIGH
                                affectedStudents > 5 -> ConflictSeverity.MEDIUM
                                else -> ConflictSeverity.LOW
                            }

                            conflicts.add(
                                TimeConflict(
                                    examId1 = exam1.scheduledExamId,
                                    examId2 = exam2.scheduledExamId,
                                    conflictType = "TIME_OVERLAP",
                                    severity = severity,
                                    affectedStudents = affectedStudents
                                )
                            )
                        }
                    }

                    val timeBetween = calculateTimeBetween(exam1, exam2)
                    if (timeBetween != null && timeBetween.toMinutes() < 30) {
                        conflicts.add(
                            TimeConflict(
                                examId1 = exam1.scheduledExamId,
                                examId2 = exam2.scheduledExamId,
                                conflictType = "INSUFFICIENT_BREAK",
                                severity = ConflictSeverity.MEDIUM,
                                affectedStudents = estimateStudentOverlap(exam1, exam2, enrollmentData)
                            )
                        )
                    }
                }
            }
        }

        return conflicts
    }

    private fun detectSpaceConflicts(scheduledExams: List<ScheduledExam>): List<SpaceConflict> {
        val conflicts = mutableListOf<SpaceConflict>()

        scheduledExams.forEach { exam ->
            if (exam.roomCapacity != null && exam.studentCount > exam.roomCapacity) {
                conflicts.add(
                    SpaceConflict(
                        examId = exam.scheduledExamId,
                        roomId = exam.roomId ?: "unknown",
                        requiredCapacity = exam.studentCount,
                        availableCapacity = exam.roomCapacity,
                        overflowCount = exam.studentCount - exam.roomCapacity
                    )
                )
            }
        }

        val examsByRoom = scheduledExams
            .filter { it.roomId != null }
            .groupBy { it.roomId!! }

        examsByRoom.forEach { (roomId, examsInRoom) ->
            val examsByDate = examsInRoom.groupBy { it.examDate }

            examsByDate.forEach { (date, examsOnDate) ->
                for (i in examsOnDate.indices) {
                    for (j in i + 1 until examsOnDate.size) {
                        val exam1 = examsOnDate[i]
                        val exam2 = examsOnDate[j]

                        if (timeSlotsOverlap(exam1.startTime, exam1.endTime, exam2.startTime, exam2.endTime)) {
                            conflicts.add(
                                SpaceConflict(
                                    examId = exam1.scheduledExamId,
                                    roomId = roomId,
                                    requiredCapacity = exam1.studentCount + exam2.studentCount,
                                    availableCapacity = exam1.roomCapacity ?: 0,
                                    overflowCount = -1
                                )
                            )
                        }
                    }
                }
            }
        }

        return conflicts
    }

    private fun detectProfessorConflicts(scheduledExams: List<ScheduledExam>): List<ProfessorConflict> {
        val conflicts = mutableListOf<ProfessorConflict>()

        val examsByProfessor = mutableMapOf<String, MutableList<ScheduledExam>>()

        scheduledExams.forEach { exam ->
            exam.professorIds.forEach { professorId ->
                examsByProfessor.computeIfAbsent(professorId) { mutableListOf() }.add(exam)
            }
        }

        examsByProfessor.forEach { (professorId, professorExams) ->
            val examsByDate = professorExams.groupBy { it.examDate }

            examsByDate.forEach { (date, examsOnDate) ->
                if (examsOnDate.size > 1) {
                    for (i in examsOnDate.indices) {
                        for (j in i + 1 until examsOnDate.size) {
                            val exam1 = examsOnDate[i]
                            val exam2 = examsOnDate[j]

                            if (timeSlotsOverlap(exam1.startTime, exam1.endTime, exam2.startTime, exam2.endTime)) {
                                conflicts.add(
                                    ProfessorConflict(
                                        professorId = professorId,
                                        conflictingExamIds = listOf(exam1.scheduledExamId, exam2.scheduledExamId),
                                        conflictTime = "$date ${exam1.startTime}-${exam1.endTime} vs ${exam2.startTime}-${exam2.endTime}",
                                        severity = ConflictSeverity.CRITICAL
                                    )
                                )
                            }
                        }
                    }
                }

                if (examsOnDate.size > 3) {
                    conflicts.add(
                        ProfessorConflict(
                            professorId = professorId,
                            conflictingExamIds = examsOnDate.map { it.scheduledExamId },
                            conflictTime = "$date (${examsOnDate.size} exams)",
                            severity = ConflictSeverity.HIGH
                        )
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * Stores detected conflicts in the database for tracking and resolution.
     */
    @Transactional
    fun storeDetectedConflicts(
        scheduleId: UUID,
        timeConflicts: List<TimeConflict>,
        spaceConflicts: List<SpaceConflict>,
        professorConflicts: List<ProfessorConflict>
    ) {
        scheduleConflictRepository.findByScheduleId(scheduleId).forEach { conflict ->
            scheduleConflictRepository.delete(conflict)
        }

        // Store time conflicts
        timeConflicts.forEach { conflict ->
            val entity = ScheduleConflictEntity(
                scheduleId = scheduleId,
                conflictId = "TIME_${conflict.examId1}_${conflict.examId2}",
                conflictType = ConflictType.TIME_OVERLAP,
                severity = conflict.severity,
                description = "Time conflict between exams ${conflict.examId1} and ${conflict.examId2}",
                affectedExamIds = mutableSetOf(conflict.examId1, conflict.examId2),
                affectedStudents = conflict.affectedStudents,
                suggestedResolution = "Reschedule one exam to a different time slot"
            )
            scheduleConflictRepository.save(entity)
        }

        spaceConflicts.forEach { conflict ->
            val entity = ScheduleConflictEntity(
                scheduleId = scheduleId,
                conflictId = "SPACE_${conflict.examId}_${conflict.roomId}",
                conflictType = ConflictType.ROOM_CAPACITY,
                severity = if (conflict.overflowCount > 20) ConflictSeverity.CRITICAL else ConflictSeverity.HIGH,
                description = if (conflict.overflowCount == -1) {
                    "Room ${conflict.roomId} double-booked for exam ${conflict.examId}"
                } else {
                    "Room ${conflict.roomId} capacity exceeded by ${conflict.overflowCount} students for exam ${conflict.examId}"
                },
                affectedExamIds = mutableSetOf(conflict.examId),
                affectedStudents = conflict.overflowCount.takeIf { it > 0 } ?: 0,
                suggestedResolution = if (conflict.overflowCount == -1) {
                    "Reschedule one exam to a different room or time"
                } else {
                    "Assign exam to a larger room or split into multiple sessions"
                }
            )
            scheduleConflictRepository.save(entity)
        }

        professorConflicts.forEach { conflict ->
            val entity = ScheduleConflictEntity(
                scheduleId = scheduleId,
                conflictId = "PROF_${conflict.professorId}_${UUID.randomUUID()}",
                conflictType = ConflictType.PROFESSOR_AVAILABILITY,
                severity = conflict.severity,
                description = "Professor ${conflict.professorId} has conflicting exams at ${conflict.conflictTime}",
                affectedExamIds = conflict.conflictingExamIds.toMutableSet(),
                affectedStudents = 0,
                suggestedResolution = "Reschedule conflicting exams or assign additional professors"
            )
            scheduleConflictRepository.save(entity)
        }

        logger.info("Stored {} conflicts for schedule: {}",
            timeConflicts.size + spaceConflicts.size + professorConflicts.size, scheduleId)
    }


    fun getCurrentConflicts(scheduleId: UUID): List<ScheduleConflictEntity> {
        return scheduleConflictRepository.findByScheduleIdAndStatus(scheduleId, ConflictStatus.DETECTED)
    }

    @Transactional
    fun resolveConflict(conflictId: String, resolvedBy: String, resolutionNotes: String? = null) {
        scheduleConflictRepository.findByConflictId(conflictId)?.let { conflict ->
            val updatedConflict = conflict.copy(
                status = ConflictStatus.RESOLVED,
                resolvedAt = java.time.Instant.now(),
                resolvedBy = resolvedBy,
                suggestedResolution = resolutionNotes ?: conflict.suggestedResolution
            )
            scheduleConflictRepository.save(updatedConflict)

            logger.info("Conflict {} resolved by {}", conflictId, resolvedBy)
        }
    }

    fun analyzeChangeImpact(
        scheduleId: UUID,
        scheduledExamId: String,
        proposedChanges: ExamChangeProposal
    ): ChangeImpactAnalysis {
        logger.debug("Analyzing impact of proposed changes for exam: {}", scheduledExamId)

        val currentExams = scheduledExamRepository.findByExamSessionScheduleId(scheduleId)
        val targetExam = currentExams.find { it.scheduledExamId == scheduledExamId }
            ?: throw IllegalArgumentException("Exam not found: $scheduledExamId")

        val modifiedExam = applyProposedChanges(targetExam, proposedChanges)
        val modifiedExamList = currentExams.map {
            if (it.scheduledExamId == scheduledExamId) modifiedExam else it
        }

        val newConflicts = detectTimeConflicts(modifiedExamList, emptyMap()) // Simplified for impact analysis
        val currentConflicts = detectTimeConflicts(currentExams, emptyMap())

        val conflictsResolved = currentConflicts.filter { oldConflict ->
            newConflicts.none { newConflict ->
                (newConflict.examId1 == oldConflict.examId1 && newConflict.examId2 == oldConflict.examId2) ||
                        (newConflict.examId1 == oldConflict.examId2 && newConflict.examId2 == oldConflict.examId1)
            }
        }

        val conflictsCreated = newConflicts.filter { newConflict ->
            currentConflicts.none { oldConflict ->
                (oldConflict.examId1 == newConflict.examId1 && oldConflict.examId2 == newConflict.examId2) ||
                        (oldConflict.examId1 == newConflict.examId2 && oldConflict.examId2 == newConflict.examId1)
            }
        }

        return ChangeImpactAnalysis(
            examId = scheduledExamId,
            proposedChanges = proposedChanges,
            conflictsResolved = conflictsResolved.map { "${it.examId1}-${it.examId2}" },
            conflictsCreated = conflictsCreated.map { "${it.examId1}-${it.examId2}" },
            impactedStudents = (conflictsCreated.sumOf { it.affectedStudents } -
                    conflictsResolved.sumOf { it.affectedStudents }),
            recommendationScore = calculateRecommendationScore(conflictsResolved.size, conflictsCreated.size),
            additionalImpacts = analyzeAdditionalImpacts(targetExam, modifiedExam)
        )
    }

    private fun timeSlotsOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean {
        return start1.isBefore(end2) && start2.isBefore(end1)
    }

    private fun calculateTimeBetween(exam1: ScheduledExam, exam2: ScheduledExam): Duration? {
        if (exam1.examDate != exam2.examDate) return null

        return when {
            exam1.endTime.isBefore(exam2.startTime) -> Duration.between(exam1.endTime, exam2.startTime)
            exam2.endTime.isBefore(exam1.startTime) -> Duration.between(exam2.endTime, exam1.startTime)
            else -> null // Overlapping
        }
    }

    private fun estimateStudentOverlap(
        exam1: ScheduledExam,
        exam2: ScheduledExam,
        enrollmentData: Map<String, CourseEnrollmentInfo>
    ): Int {
        val course1Students = enrollmentData[exam1.courseId]?.studentCount ?: exam1.studentCount
        val course2Students = enrollmentData[exam2.courseId]?.studentCount ?: exam2.studentCount

        val overlapRate = when {
            exam1.mandatoryStatus == MandatoryStatus.MANDATORY &&
                    exam2.mandatoryStatus == MandatoryStatus.MANDATORY -> 0.7
            exam1.mandatoryStatus == MandatoryStatus.MANDATORY ||
                    exam2.mandatoryStatus == MandatoryStatus.MANDATORY -> 0.4
            else -> 0.2
        }

        return (minOf(course1Students, course2Students) * overlapRate).toInt()
    }

    private fun countCriticalViolations(
        timeConflicts: List<TimeConflict>,
        spaceConflicts: List<SpaceConflict>,
        professorConflicts: List<ProfessorConflict>
    ): Int {
        return timeConflicts.count { it.severity == ConflictSeverity.CRITICAL } +
                spaceConflicts.count { it.overflowCount > 20 || it.overflowCount == -1 } +
                professorConflicts.count { it.severity == ConflictSeverity.CRITICAL }
    }

    private fun applyProposedChanges(exam: ScheduledExam, changes: ExamChangeProposal): ScheduledExam {
        return exam.copy(
            examDate = changes.newExamDate ?: exam.examDate,
            startTime = changes.newStartTime ?: exam.startTime,
            endTime = changes.newEndTime ?: exam.endTime,
            roomId = changes.newRoomId ?: exam.roomId,
            roomName = changes.newRoomName ?: exam.roomName,
            roomCapacity = changes.newRoomCapacity ?: exam.roomCapacity
        )
    }

    private fun calculateRecommendationScore(conflictsResolved: Int, conflictsCreated: Int): Double {
        return when {
            conflictsCreated == 0 && conflictsResolved > 0 -> 1.0
            conflictsCreated == 0 && conflictsResolved == 0 -> 0.8
            conflictsResolved > conflictsCreated -> 0.6
            conflictsResolved == conflictsCreated -> 0.4
            else -> 0.2
        }
    }

    private fun analyzeAdditionalImpacts(original: ScheduledExam, modified: ScheduledExam): List<String> {
        val impacts = mutableListOf<String>()

        if (original.examDate != modified.examDate) {
            impacts.add("Date change may affect student travel arrangements")
        }

        if (original.roomId != modified.roomId) {
            impacts.add("Room change may require different equipment setup")
        }

        if ((original.roomCapacity ?: 0) != (modified.roomCapacity ?: 0)) {
            val diff = (modified.roomCapacity ?: 0) - (original.roomCapacity ?: 0)
            if (diff > 0) {
                impacts.add("Increased room capacity provides $diff additional seats")
            } else {
                impacts.add("Reduced room capacity removes ${-diff} seats")
            }
        }

        return impacts
    }

    @Autowired
    private lateinit var scheduledExamRepository: ScheduledExamRepository
}