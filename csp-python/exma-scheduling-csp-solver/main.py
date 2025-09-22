from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import datetime, date, time, timedelta
from enum import Enum
import time as time_module
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Exam Scheduling Service", version="1.0.0")

class MandatoryStatus(str, Enum):
    MANDATORY = "MANDATORY"
    ELECTIVE = "ELECTIVE"

class ViolationSeverity(str, Enum):
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"

# Request Models - Keep simple and working
class ExamPeriod(BaseModel):
    examSessionPeriodId: str
    academicYear: str
    examSession: str
    startDate: date
    endDate: date

class CourseSchedulingInfo(BaseModel):
    courseId: str
    courseName: str
    studentCount: int
    professorIds: List[str]
    mandatoryStatus: MandatoryStatus
    estimatedDuration: int
    requiredEquipment: List[str] = []
    accessibilityRequired: bool = False
    specialRequirements: Optional[str] = None

class RoomInfo(BaseModel):
    roomId: str
    roomName: str
    capacity: int
    equipment: List[str] = []
    location: Optional[str] = None
    accessibility: bool = True
    availableTimeSlots: List[Dict] = []

class ProfessorPreferenceInfo(BaseModel):
    preferenceId: Optional[str] = None
    professorId: str
    courseId: str
    preferredDates: List[date] = []
    preferredTimeSlots: List[Dict] = []
    unavailableDates: List[date] = []
    unavailableTimeSlots: List[Dict] = []
    preferredRooms: List[str] = []
    specialRequirements: Optional[str] = None
    priority: int = 1

class WorkingHours(BaseModel):
    startTime: time
    endTime: time

class InstitutionalConstraints(BaseModel):
    workingHours: WorkingHours
    minimumExamDuration: int
    minimumGapMinutes: int
    maxExamsPerDay: int
    maxExamsPerRoom: int
    allowWeekendExams: bool

class PythonSchedulingRequest(BaseModel):
    examPeriod: ExamPeriod
    courses: List[CourseSchedulingInfo]
    availableRooms: List[RoomInfo]
    professorPreferences: List[ProfessorPreferenceInfo]
    institutionalConstraints: InstitutionalConstraints

# Response Models
class ScheduledExamInfo(BaseModel):
    scheduledExamId: str
    courseId: str
    courseName: str
    examDate: date
    startTime: time
    endTime: time
    roomId: str
    roomName: str
    roomCapacity: Optional[int]
    studentCount: int
    mandatoryStatus: MandatoryStatus
    professorIds: List[str]

class PythonConstraintViolation(BaseModel):
    violationType: str
    severity: str
    description: str
    affectedExamIds: List[str]
    affectedStudents: int
    suggestedResolution: Optional[str] = None

class PythonSchedulingMetrics(BaseModel):
    totalCoursesScheduled: int
    totalProfessorPreferencesConsidered: int
    preferencesSatisfied: int
    preferenceSatisfactionRate: float
    totalConflicts: int
    resolvedConflicts: int
    roomUtilizationRate: float
    averageStudentExamsPerDay: float
    processingTimeMs: int

class PythonSchedulingResponse(BaseModel):
    success: bool
    errorMessage: Optional[str] = None
    scheduledExams: List[ScheduledExamInfo]
    metrics: PythonSchedulingMetrics
    qualityScore: float
    violations: List[PythonConstraintViolation]
    processingTimeMs: int = 0
    algorithmUsed: Optional[str] = None

class HealthResponse(BaseModel):
    status: str
    timestamp: str
    version: str
    uptime: int

# Scheduling Algorithm
class ExamScheduler:
    def __init__(self, request: PythonSchedulingRequest):
        self.request = request
        self.scheduled_exams = []
        self.violations = []
        self.room_usage = {}
        self.professor_schedules = {}
        self.preferences_considered = 0
        self.preferences_satisfied = 0

    def generate_schedule(self) -> PythonSchedulingResponse:
        start_time = time_module.time()

        try:
            logger.info(f"=== STARTING SCHEDULE GENERATION ===")
            logger.info(f"Courses to schedule: {len(self.request.courses)}")
            logger.info(f"Available rooms: {len(self.request.availableRooms)}")
            logger.info(f"Professor preferences: {len(self.request.professorPreferences)}")

            # Sort courses by priority (mandatory first, then by student count)
            sorted_courses = sorted(
                self.request.courses,
                key=lambda c: (
                    0 if c.mandatoryStatus == MandatoryStatus.MANDATORY else 1,
                    -c.studentCount
                )
            )

            logger.info(f"Course scheduling order: {[c.courseId for c in sorted_courses]}")

            # Schedule each course
            for course in sorted_courses:
                success = self._schedule_course(course)
                logger.info(f"Course {course.courseId} scheduling: {'SUCCESS' if success else 'FAILED'}")

            # Calculate metrics
            processing_time = int((time_module.time() - start_time) * 1000)
            metrics = self._calculate_metrics(processing_time)
            quality_score = self._calculate_quality_score()

            logger.info(f"=== SCHEDULE GENERATION COMPLETE ===")
            logger.info(f"Scheduled {len(self.scheduled_exams)} out of {len(self.request.courses)} courses")
            logger.info(f"Quality score: {quality_score}")
            logger.info(f"Processing time: {processing_time}ms")

            return PythonSchedulingResponse(
                success=True,
                scheduledExams=self.scheduled_exams,
                metrics=metrics,
                qualityScore=quality_score,
                violations=self.violations,
                processingTimeMs=processing_time,
                algorithmUsed="Greedy Constraint Satisfaction"
            )

        except Exception as e:
            processing_time = int((time_module.time() - start_time) * 1000)
            logger.error(f"Schedule generation failed: {str(e)}")
            import traceback
            traceback.print_exc()
            return PythonSchedulingResponse(
                success=False,
                errorMessage=str(e),
                scheduledExams=[],
                metrics=PythonSchedulingMetrics(
                    totalCoursesScheduled=0,
                    totalProfessorPreferencesConsidered=0,
                    preferencesSatisfied=0,
                    preferenceSatisfactionRate=0.0,
                    totalConflicts=0,
                    resolvedConflicts=0,
                    roomUtilizationRate=0.0,
                    averageStudentExamsPerDay=0.0,
                    processingTimeMs=processing_time
                ),
                qualityScore=0.0,
                violations=[],
                processingTimeMs=processing_time
            )

    def _schedule_course(self, course: CourseSchedulingInfo) -> bool:
        logger.info(f"--- Scheduling {course.courseId} ({course.courseName}) ---")
        logger.info(f"Student count: {course.studentCount}, Duration: {course.estimatedDuration}min")

        # Get professor preferences for this course
        course_preferences = [p for p in self.request.professorPreferences if p.courseId == course.courseId]
        self.preferences_considered += len(course_preferences)
        logger.info(f"Found {len(course_preferences)} preferences for {course.courseId}")

        # Find suitable room
        suitable_room = self._find_suitable_room(course)
        if not suitable_room:
            logger.error(f"❌ No suitable room found for {course.courseId}")
            self.violations.append(PythonConstraintViolation(
                violationType="NO_SUITABLE_ROOM",
                severity=ViolationSeverity.CRITICAL,
                description=f"No suitable room found for course {course.courseId} with {course.studentCount} students",
                affectedExamIds=[course.courseId],
                affectedStudents=course.studentCount,
                suggestedResolution="Add more rooms or reduce class size"
            ))
            return False

        logger.info(f"✅ Selected room: {suitable_room.roomName} (capacity: {suitable_room.capacity})")

        # Find suitable time slot
        time_slot = self._find_suitable_time_slot(course, suitable_room, course_preferences)
        if not time_slot:
            logger.error(f"❌ No suitable time slot found for {course.courseId}")
            self.violations.append(PythonConstraintViolation(
                violationType="NO_SUITABLE_TIME_SLOT",
                severity=ViolationSeverity.CRITICAL,
                description=f"No suitable time slot found for course {course.courseId}",
                affectedExamIds=[course.courseId],
                affectedStudents=course.studentCount,
                suggestedResolution="Extend exam period or reduce constraints"
            ))
            return False

        exam_date, start_time, end_time = time_slot
        logger.info(f"✅ Selected time slot: {exam_date} {start_time}-{end_time}")

        # Create scheduled exam
        scheduled_exam = ScheduledExamInfo(
            scheduledExamId=f"{course.courseId}_{exam_date.strftime('%Y%m%d')}_{start_time.strftime('%H%M')}",
            courseId=course.courseId,
            courseName=course.courseName,
            examDate=exam_date,
            startTime=start_time,
            endTime=end_time,
            roomId=suitable_room.roomId,
            roomName=suitable_room.roomName,
            roomCapacity=suitable_room.capacity,
            studentCount=course.studentCount,
            mandatoryStatus=course.mandatoryStatus,
            professorIds=course.professorIds
        )

        self.scheduled_exams.append(scheduled_exam)

        # Update room usage tracking
        room_key = f"{suitable_room.roomId}_{exam_date}_{start_time}"
        self.room_usage[room_key] = scheduled_exam

        # Update professor schedules tracking
        for prof_id in course.professorIds:
            if prof_id not in self.professor_schedules:
                self.professor_schedules[prof_id] = []
            self.professor_schedules[prof_id].append(scheduled_exam)

        # Check if preferences were satisfied
        if self._check_preferences_satisfied(scheduled_exam, course_preferences):
            self.preferences_satisfied += 1
            logger.info(f"✅ Preferences satisfied for {course.courseId}")
        else:
            logger.info(f"⚠️  Preferences not fully satisfied for {course.courseId}")

        logger.info(f"✅ Successfully scheduled {course.courseId}")
        return True

    def _find_suitable_room(self, course: CourseSchedulingInfo) -> Optional[RoomInfo]:
        logger.info(f"Finding room for {course.courseId} (needs {course.studentCount} capacity)")

        # Find rooms with sufficient capacity
        suitable_rooms = [r for r in self.request.availableRooms if r.capacity >= course.studentCount]
        logger.info(f"Rooms with sufficient capacity: {[f'{r.roomId}({r.capacity})' for r in suitable_rooms]}")

        if not suitable_rooms:
            return None

        # Check equipment requirements
        if course.requiredEquipment:
            logger.info(f"Filtering by equipment: {course.requiredEquipment}")
            suitable_rooms = [r for r in suitable_rooms
                              if all(eq in r.equipment for eq in course.requiredEquipment)]
            logger.info(f"After equipment filter: {[r.roomId for r in suitable_rooms]}")

        # Check accessibility requirements
        if course.accessibilityRequired:
            logger.info("Filtering by accessibility requirements")
            suitable_rooms = [r for r in suitable_rooms if r.accessibility]
            logger.info(f"After accessibility filter: {[r.roomId for r in suitable_rooms]}")

        if not suitable_rooms:
            return None

        # Return room with smallest sufficient capacity
        selected_room = min(suitable_rooms, key=lambda r: r.capacity)
        return selected_room

    def _find_suitable_time_slot(self, course: CourseSchedulingInfo, room: RoomInfo, preferences: List[ProfessorPreferenceInfo]):
        duration_hours = course.estimatedDuration // 60
        duration_minutes = course.estimatedDuration % 60

        logger.info(f"Finding time slot - duration: {duration_hours}h {duration_minutes}m")
        logger.info(f"Exam period: {self.request.examPeriod.startDate} to {self.request.examPeriod.endDate}")

        # Generate time slots within exam period
        current_date = self.request.examPeriod.startDate
        end_date = self.request.examPeriod.endDate

        while current_date <= end_date:
            logger.info(f"Checking date: {current_date}")

            # Skip weekends if not allowed
            if not self.request.institutionalConstraints.allowWeekendExams and current_date.weekday() >= 5:
                logger.info(f"Skipping weekend: {current_date}")
                current_date += timedelta(days=1)
                continue

            # Generate time slots for this day
            current_time = self.request.institutionalConstraints.workingHours.startTime
            end_work_time = self.request.institutionalConstraints.workingHours.endTime

            while True:
                # Calculate end time for this slot
                exam_end_time = (datetime.combine(date.today(), current_time) +
                                 timedelta(hours=duration_hours, minutes=duration_minutes)).time()

                # Check if exam fits within working hours
                if exam_end_time > end_work_time:
                    break

                # Check if this slot is available
                if self._is_time_slot_available(current_date, current_time, exam_end_time, room, course.professorIds):
                    logger.info(f"✅ Found available slot: {current_date} {current_time}-{exam_end_time}")
                    return (current_date, current_time, exam_end_time)

                # Move to next time slot
                next_time = (datetime.combine(date.today(), current_time) +
                             timedelta(minutes=self.request.institutionalConstraints.minimumGapMinutes)).time()

                if next_time >= end_work_time:
                    break

                current_time = next_time

            current_date += timedelta(days=1)

        logger.info("❌ No suitable time slot found")
        return None

    def _is_time_slot_available(self, exam_date: date, start_time: time, end_time: time,
                                room: RoomInfo, professor_ids: List[str]) -> bool:

        # Check room availability
        for existing_exam in self.scheduled_exams:
            if (existing_exam.roomId == room.roomId and
                    existing_exam.examDate == exam_date and
                    self._times_overlap(start_time, end_time, existing_exam.startTime, existing_exam.endTime)):
                logger.info(f"Room conflict with {existing_exam.courseId}")
                return False

        # Check professor availability
        for prof_id in professor_ids:
            if prof_id in self.professor_schedules:
                for existing_exam in self.professor_schedules[prof_id]:
                    if (existing_exam.examDate == exam_date and
                            self._times_overlap(start_time, end_time, existing_exam.startTime, existing_exam.endTime)):
                        logger.info(f"Professor {prof_id} conflict with {existing_exam.courseId}")
                        return False

        return True

    def _times_overlap(self, start1: time, end1: time, start2: time, end2: time) -> bool:
        return start1 < end2 and start2 < end1

    def _check_preferences_satisfied(self, exam: ScheduledExamInfo, preferences: List[ProfessorPreferenceInfo]) -> bool:
        if not preferences:
            return True

        for pref in preferences:
            # Check preferred dates
            if pref.preferredDates and exam.examDate in pref.preferredDates:
                return True

            # Check preferred time slots
            if pref.preferredTimeSlots:
                for time_slot in pref.preferredTimeSlots:
                    slot_start = datetime.strptime(time_slot.get('startTime', '00:00:00'), '%H:%M:%S').time()
                    slot_end = datetime.strptime(time_slot.get('endTime', '23:59:59'), '%H:%M:%S').time()
                    if exam.startTime >= slot_start and exam.endTime <= slot_end:
                        return True

            # Check preferred rooms
            if pref.preferredRooms and exam.roomId in pref.preferredRooms:
                return True

        return False

    def _calculate_metrics(self, processing_time: int) -> PythonSchedulingMetrics:
        total_days = (self.request.examPeriod.endDate - self.request.examPeriod.startDate).days + 1
        avg_exams_per_day = len(self.scheduled_exams) / total_days if total_days > 0 else 0

        # Calculate room utilization
        room_utilization = 0.0
        if self.scheduled_exams:
            total_capacity = sum(exam.roomCapacity or 0 for exam in self.scheduled_exams)
            total_students = sum(exam.studentCount for exam in self.scheduled_exams)
            room_utilization = total_students / total_capacity if total_capacity > 0 else 0

        preference_satisfaction_rate = (self.preferences_satisfied / self.preferences_considered
                                        if self.preferences_considered > 0 else 0)

        return PythonSchedulingMetrics(
            totalCoursesScheduled=len(self.scheduled_exams),
            totalProfessorPreferencesConsidered=self.preferences_considered,
            preferencesSatisfied=self.preferences_satisfied,
            preferenceSatisfactionRate=preference_satisfaction_rate,
            totalConflicts=len(self.violations),
            resolvedConflicts=0,
            roomUtilizationRate=room_utilization,
            averageStudentExamsPerDay=avg_exams_per_day,
            processingTimeMs=processing_time
        )

    def _calculate_quality_score(self) -> float:
        if not self.scheduled_exams:
            return 0.0

        # Base score from successful scheduling
        base_score = len(self.scheduled_exams) / len(self.request.courses)

        # Preference satisfaction bonus
        pref_bonus = (self.preferences_satisfied / self.preferences_considered
                      if self.preferences_considered > 0 else 0) * 0.3

        # Penalty for violations
        violation_penalty = len(self.violations) * 0.1

        return max(0.0, min(1.0, base_score + pref_bonus - violation_penalty))

# API Endpoints
@app.get("/api/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(
        status="healthy",
        timestamp=datetime.now().isoformat(),
        version="1.0.0",
        uptime=3600
    )

@app.post("/api/schedule/generate", response_model=PythonSchedulingResponse)
async def generate_schedule(request: PythonSchedulingRequest):
    try:
        logger.info(f"Received scheduling request for {len(request.courses)} courses")
        scheduler = ExamScheduler(request)
        response = scheduler.generate_schedule()
        logger.info(f"Generated schedule with {len(response.scheduledExams)} exams")
        return response
    except Exception as e:
        logger.error(f"Error generating schedule: {str(e)}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8009)