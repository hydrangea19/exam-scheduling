package mk.ukim.finki.examscheduling.usermanagement.query.handler

import jakarta.validation.ConstraintViolationException
import mk.ukim.finki.examscheduling.usermanagement.domain.exceptions.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.lang.SecurityException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: ValidationException): ValidationErrorResponse {
        logger.warn("Validation error: {}", ex.message)

        return ValidationErrorResponse(
            message = ex.message ?: "Validation failed",
            fieldErrors = ex.fieldErrors
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ValidationErrorResponse {
        logger.warn("Method argument validation failed: {}", ex.message)

        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        return ValidationErrorResponse(
            message = "Request validation failed",
            fieldErrors = fieldErrors
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ValidationErrorResponse {
        logger.warn("Constraint validation failed: {}", ex.message)

        val fieldErrors = ex.constraintViolations.associate { violation ->
            violation.propertyPath.toString() to violation.message
        }

        return ValidationErrorResponse(
            message = "Request validation failed",
            fieldErrors = fieldErrors
        )
    }

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleUserNotFoundException(ex: UserNotFoundException): ErrorResponse {
        logger.warn("User not found: {}", ex.message)

        return ErrorResponse(
            error = "User Not Found",
            message = ex.message ?: "User not found",
            details = buildMap {
                ex.userId?.let { put("userId", it) }
                ex.email?.let { put("email", it) }
            }
        )
    }

    @ExceptionHandler(AuthorizationException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAuthorizationException(ex: AuthorizationException): ErrorResponse {
        logger.warn("Authorization error: {}", ex.message)

        return ErrorResponse(
            error = "Access Denied",
            message = ex.message ?: "Access denied",
            details = buildMap {
                ex.requiredRole?.let { put("requiredRole", it) }
                ex.currentRole?.let { put("currentRole", it) }
            }
        )
    }

    @ExceptionHandler(SecurityException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleSecurityException(ex: SecurityException): ErrorResponse {
        logger.warn("Security error: {}", ex.message)

        return ErrorResponse(
            error = "Security Error",
            message = ex.message ?: "Authentication required"
        )
    }

    @ExceptionHandler(BusinessRuleViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleBusinessRuleViolationException(ex: BusinessRuleViolationException): ErrorResponse {
        logger.warn("Business rule violation: {}", ex.message)

        return ErrorResponse(
            error = "Business Rule Violation",
            message = ex.message ?: "Business rule violation",
            details = ex.ruleViolated?.let { mapOf("ruleViolated" to it) }
        )
    }

    @ExceptionHandler(UserDomainException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUserDomainException(ex: UserDomainException): ErrorResponse {
        logger.warn("Domain validation error: {}", ex.message)

        return ErrorResponse(
            error = "Domain Validation Error",
            message = ex.message ?: "Domain validation failed"
        )
    }

    @ExceptionHandler(InternalServerException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleInternalServerException(ex: InternalServerException): ErrorResponse {
        logger.error("Internal server error: {}", ex.message, ex)

        return ErrorResponse(
            error = "Internal Server Error",
            message = "An internal error occurred"
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun handleAccessDeniedException(ex: AccessDeniedException): ErrorResponse {
        logger.warn("Spring Security access denied: {}", ex.message)

        return ErrorResponse(
            error = "Access Denied",
            message = "Insufficient permissions to access this resource"
        )
    }

    @ExceptionHandler(AuthenticationException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleAuthenticationException(ex: AuthenticationException): ErrorResponse {
        logger.warn("Authentication error: {}", ex.message)

        return ErrorResponse(
            error = "Authentication Required",
            message = "Valid authentication credentials are required"
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ErrorResponse {
        logger.warn("HTTP message not readable: {}", ex.message)

        return ErrorResponse(
            error = "Invalid Request",
            message = "Request body could not be parsed"
        )
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ErrorResponse {
        logger.warn("Method argument type mismatch: {}", ex.message)

        return ErrorResponse(
            error = "Invalid Request Parameter",
            message = "Parameter '${ex.name}' has invalid format"
        )
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(ex: Exception): ErrorResponse {
        logger.error("Unexpected error occurred", ex)

        return ErrorResponse(
            error = "Internal Server Error",
            message = "An unexpected error occurred"
        )
    }
}