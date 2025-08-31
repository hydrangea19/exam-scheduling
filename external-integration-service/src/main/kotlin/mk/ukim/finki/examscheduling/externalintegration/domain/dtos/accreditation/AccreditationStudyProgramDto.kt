package mk.ukim.finki.examscheduling.externalintegration.domain.dtos.accreditation

data class AccreditationStudyProgramDto(
    val code: String,
    val name: String,
    val nameEn: String,
    val durationYears: Int?,
    val generalInformation: String?,
    val graduationTitle: String?,
    val graduationTitleEn: String?,
    val subjectRestrictions: String?,
    val inEnglish: Boolean = false,
    val studyCycle: String,
    val accreditation: String,
    val bilingual: Boolean = false,
    val coordinator: String? = null
)