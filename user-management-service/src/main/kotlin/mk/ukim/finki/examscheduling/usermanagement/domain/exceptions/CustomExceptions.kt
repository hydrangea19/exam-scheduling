package mk.ukim.finki.examscheduling.usermanagement.domain.exceptions

import java.util.*

abstract class UserManagementException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)


class ValidationException(
    message: String,
    val fieldErrors: Map<String, String>? = null,
    cause: Throwable? = null
) : UserManagementException(message, cause)


class InternalServerException(
    message: String,
    cause: Throwable? = null
) : UserManagementException(message, cause)


class SecurityException(
    message: String,
    cause: Throwable? = null
) : UserManagementException(message, cause)


class UserNotFoundException(
    message: String,
    val userId: UUID? = null,
    val email: String? = null,
    cause: Throwable? = null
) : UserManagementException(message, cause)

class BusinessRuleViolationException(
    message: String,
    val ruleViolated: String? = null,
    cause: Throwable? = null
) : UserManagementException(message, cause)

class AuthorizationException(
    message: String,
    val requiredRole: String? = null,
    val currentRole: String? = null,
    cause: Throwable? = null
) : UserManagementException(message, cause)