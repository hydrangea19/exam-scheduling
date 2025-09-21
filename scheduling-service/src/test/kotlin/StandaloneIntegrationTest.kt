package mk.ukim.finki.examscheduling.schedulingservice

import mk.ukim.finki.examscheduling.schedulingservice.domain.*
import mk.ukim.finki.examscheduling.schedulingservice.domain.enums.MandatoryStatus
import mk.ukim.finki.examscheduling.schedulingservice.service.AdvancedSchedulingService
import mk.ukim.finki.examscheduling.schedulingservice.service.PythonSchedulingClient
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalTime

class StandaloneIntegrationTest {

    @Test
    fun `test python integration without spring context`() {
        // Create WebClient for Python service
        val webClient = WebClient.builder()
            .baseUrl("http://localhost:8009")
            .build()

        // Create real PythonSchedulingClient
        val pythonClient = PythonSchedulingClient(webClient)


        // Create service with dependencies
        val schedulingService = AdvancedSchedulingService(pythonClient)

        // Test data
        val examPeriod = ExamPeriod(
            examSessionPeriodId = "TEST_2025",
            academicYear = "2024-2025",
            examSession = "Test",
            startDate = LocalDate.of(2025, 6, 15),
            endDate = LocalDate.of(2025, 6, 20)
        )

        val courseEnrollmentData = mapOf(
            "CS101" to CourseEnrollmentInfo(
                courseId = "CS101",
                studentCount = 50,
                enrollmentDetails = mapOf()
            ),
            "MATH201" to CourseEnrollmentInfo(
                courseId = "MATH201",
                studentCount = 30,
                enrollmentDetails = mapOf()
            )
        )

        val courseAccreditationData = mapOf(
            "CS101" to CourseAccreditationInfo(
                courseId = "CS101",
                courseName = "Computer Science",
                credits = 6,
                professorIds = setOf("PROF001"),
                mandatoryStatus = MandatoryStatus.MANDATORY,
                accreditationDetails = mapOf()
            ),
            "MATH201" to CourseAccreditationInfo(
                courseId = "MATH201",
                courseName = "Mathematics",
                credits = 5,
                professorIds = setOf("PROF002"),
                mandatoryStatus = MandatoryStatus.MANDATORY,
                accreditationDetails = mapOf()
            )
        )

        val availableRooms = listOf(
            RoomInfo(
                roomId = "ROOM_A101",
                roomName = "Amphitheater A101",
                capacity = 80,
                equipment = setOf("projector"),
                location = "Building A",
                accessibility = true
            ),
            RoomInfo(
                roomId = "ROOM_B205",
                roomName = "Classroom B205",
                capacity = 40,
                equipment = setOf("whiteboard"),
                location = "Building B",
                accessibility = true
            )
        )

        val professorPreferences = listOf(
            ProfessorPreferenceInfo(
                preferenceId = "PREF001",
                professorId = "PROF001",
                courseId = "CS101",
                preferredDates = listOf(LocalDate.of(2025, 6, 16)),
                preferredTimeSlots = listOf(
                    TimeSlotPreference(LocalTime.of(9, 0), LocalTime.of(11, 0))
                ),
                preferredRooms = listOf("ROOM_A101"),
                unavailableDates = emptyList(),
                unavailableTimeSlots = emptyList(),
                specialRequirements = "Morning preference",
                priority = 1
            )
        )

        // Test the integration
        println("Testing direct Python integration...")
        val startTime = System.currentTimeMillis()

        val result = schedulingService.generateOptimalSchedule(
            courseEnrollmentData = courseEnrollmentData,
            courseAccreditationData = courseAccreditationData,
            professorPreferences = professorPreferences,
            availableRooms = availableRooms,
            examPeriod = examPeriod
        )

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Results
        println("DIRECT INTEGRATION TEST RESULTS:")
        println("Processing Time: ${totalTime}ms")
        println("Scheduled Exams: ${result.scheduledExams.size}/${courseEnrollmentData.size}")
        println("Quality Score: ${"%.2f".format(result.qualityScore)}")
        println("Violations: ${result.violations.size}")

        if (result.scheduledExams.isNotEmpty()) {
            println("\nScheduled Exams:")
            result.scheduledExams.forEach { exam ->
                println("  ${exam.courseId}: ${exam.examDate} ${exam.startTime}-${exam.endTime} in ${exam.roomName}")
            }
        }

        // Assertions
        assert(result.scheduledExams.isNotEmpty()) { "Should schedule at least one exam" }
        assert(result.qualityScore > 0.0) { "Quality score should be positive" }

        println("\nDirect integration test completed successfully!")
    }
}