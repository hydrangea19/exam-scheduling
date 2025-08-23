package mk.ukim.finki.examscheduling.externalintegration.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "courses",
    indexes = [
        Index(name = "idx_courses_external_id", columnList = "external_course_id"),
        Index(name = "idx_courses_code", columnList = "course_code"),
        Index(name = "idx_courses_department", columnList = "department")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_courses_external_id", columnNames = ["external_course_id"])
    ]
)
data class Course(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "external_course_id", nullable = false, unique = true, length = 100)
    @NotBlank(message = "External course ID is required")
    @Size(max = 100, message = "External course ID cannot exceed 100 characters")
    val externalCourseId: String,

    @Column(name = "course_code", nullable = false, length = 20)
    @NotBlank(message = "Course code is required")
    @Size(max = 20, message = "Course code cannot exceed 20 characters")
    val courseCode: String,

    @Column(name = "course_name", nullable = false, length = 300)
    @NotBlank(message = "Course name is required")
    @Size(max = 300, message = "Course name cannot exceed 300 characters")
    val courseName: String,

    @Column(name = "ects_credits")
    val ectsCredits: Int? = null,

    @Column(name = "semester")
    val semester: Int? = null,

    @Column(name = "department", length = 200)
    @Size(max = 200, message = "Department cannot exceed 200 characters")
    val department: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    constructor() : this(
        id = UUID.randomUUID(),
        externalCourseId = "",
        courseCode = "",
        courseName = ""
    )

    fun getFullIdentifier(): String = "$courseCode - $courseName"

    fun getDisplayName(): String {
        return if (ectsCredits != null) {
            "$courseCode - $courseName ($ectsCredits ECTS)"
        } else {
            "$courseCode - $courseName"
        }
    }

    fun hasSemesterInfo(): Boolean = semester != null

    fun getSemesterDisplay(): String {
        return when (semester) {
            1 -> "1st Semester"
            2 -> "2nd Semester"
            3 -> "3rd Semester"
            4 -> "4th Semester"
            5 -> "5th Semester"
            6 -> "6th Semester"
            7 -> "7th Semester"
            8 -> "8th Semester"
            else -> "Unknown Semester"
        }
    }

    fun update(
        courseCode: String = this.courseCode,
        courseName: String = this.courseName,
        ectsCredits: Int? = this.ectsCredits,
        semester: Int? = this.semester,
        department: String? = this.department
    ): Course {
        return copy(
            courseCode = courseCode,
            courseName = courseName,
            ectsCredits = ectsCredits,
            semester = semester,
            department = department,
            updatedAt = Instant.now()
        )
    }

    override fun toString(): String {
        return "Course(id=$id, externalId='$externalCourseId', code='$courseCode', name='$courseName', department='$department')"
    }
}
