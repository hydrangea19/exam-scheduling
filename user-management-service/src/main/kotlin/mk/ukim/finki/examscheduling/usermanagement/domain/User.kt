package mk.ukim.finki.examscheduling.usermanagement.domain

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import mk.ukim.finki.examscheduling.usermanagement.domain.enums.UserRole
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.*


@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_active", columnList = "active")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_email", columnNames = ["email"])
    ]
)
data class User(
    @Id
    @Column(name = "id")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false, unique = true, length = 255)
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    val email: String,

    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    val firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    val lastName: String,

    @Column(name = "middle_name", length = 100)
    @Size(max = 100, message = "Middle name cannot exceed 100 characters")
    val middleName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: UserRole = UserRole.STUDENT,

    @Column(name = "password_hash")
    var passwordHash: String? = null,

    @Column(name = "active", nullable = false)
    val active: Boolean = true,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    val version: Long = 0
) {
    constructor() : this(
        id = UUID.randomUUID(),
        email = "",
        firstName = "",
        lastName = ""
    )

    fun getFullName(): String {
        return if (middleName.isNullOrBlank()) {
            "$firstName $lastName"
        } else {
            "$firstName $middleName $lastName"
        }
    }

    fun isActive(): Boolean = active

    fun update(
        email: String = this.email,
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        middleName: String? = this.middleName,
        active: Boolean = this.active
    ): User {
        return copy(
            email = email,
            firstName = firstName,
            lastName = lastName,
            middleName = middleName,
            active = active,
            updatedAt = Instant.now()
        )
    }

    override fun toString(): String {
        return "User(id=$id, email='$email', fullName='${getFullName()}', active=$active)"
    }


}
