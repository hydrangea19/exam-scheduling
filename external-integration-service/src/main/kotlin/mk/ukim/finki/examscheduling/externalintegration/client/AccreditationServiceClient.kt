package mk.ukim.finki.examscheduling.externalintegration.client

import mk.ukim.finki.examscheduling.externalintegration.config.AccreditationServiceClientConfiguration
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.accreditation.AccreditationStudyProgramDto
import mk.ukim.finki.examscheduling.externalintegration.domain.dtos.accreditation.AccreditationSubjectDto
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "accreditation-service",
    url = "\${external-services.accreditation.url:http://localhost:8081}",
    configuration = [AccreditationServiceClientConfiguration::class]
)
interface AccreditationServiceClient {

    @GetMapping("/study_programs/all")
    fun getAllStudyPrograms(): List<AccreditationStudyProgramDto>

    @GetMapping("/subjects/{code}")
    fun getSubjectByCode(@PathVariable code: String): AccreditationSubjectDto

    @GetMapping("/subjects/study_program/{programCode}")
    fun getSubjectsByStudyProgram(@PathVariable programCode: String): List<AccreditationSubjectDto>
}