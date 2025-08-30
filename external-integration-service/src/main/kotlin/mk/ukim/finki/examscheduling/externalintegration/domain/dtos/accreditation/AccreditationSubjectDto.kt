package mk.ukim.finki.examscheduling.externalintegration.domain.dtos.accreditation

data class AccreditationSubjectDto(
    val code: String,
    val name: String,
    val abbreviation: String,
    val semester: Int?,
    val professors: List<String> = emptyList(),
    val weeklyLecturesClasses: Int?,
    val weeklyAuditoriumClasses: Int?,
    val weeklyLabClasses: Int?,
    val placeholder: Boolean?,
    val nameEn: String?,
    val defaultSemester: Short?,
    val credits: Float?,
    val studyCycle: String,
    val language: String?,
    val accreditation: String,
    val obligationDuration: Map<String, Any>? = null,
    val dependencies: Map<String, Any>? = null,
    val grading: Map<String, Any>? = null
)