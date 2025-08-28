package mk.ukim.finki.examscheduling.usermanagement.domain.exceptions

class UserDomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    constructor(message: String, details: Map<String, Any>) : this(
        "$message. Details: ${details.entries.joinToString { "${it.key}: ${it.value}" }}"
    )
}