package mk.ukim.finki.examscheduling.externalintegration.client

import mk.ukim.finki.examscheduling.externalintegration.config.RaspredelbaServiceClientConfiguration
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaClassroomDto
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaProfessorDto
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.lectureplanning.RaspredelbaSemesterDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "raspredelba-service",
    url = "\${external-services.raspredelba.url:http://localhost:8082}",
    configuration = [RaspredelbaServiceClientConfiguration::class]
)
interface RaspredelbaServiceClient {

    @GetMapping("/professors/{professorId}")
    fun getProfessorById(@PathVariable professorId: String): RaspredelbaProfessorDto

    @GetMapping("/professors/available")
    fun getAvailableProfessors(): List<RaspredelbaProfessorDto>

    @GetMapping("/professors/subject/{subjectCode}")
    fun getProfessorsBySubject(@PathVariable subjectCode: String): List<RaspredelbaProfessorDto>

    @GetMapping("/classrooms/{classroomId}")
    fun getClassroomById(@PathVariable classroomId: String): RaspredelbaClassroomDto

    @GetMapping("/classrooms/available")
    fun getAvailableClassrooms(): List<RaspredelbaClassroomDto>

    @GetMapping("/semesters/current")
    fun getCurrentSemester(): RaspredelbaSemesterDto
}