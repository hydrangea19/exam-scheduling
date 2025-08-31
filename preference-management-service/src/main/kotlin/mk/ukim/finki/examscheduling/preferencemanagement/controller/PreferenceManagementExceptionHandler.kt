package mk.ukim.finki.examscheduling.preferencemanagement.controller

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant

@ControllerAdvice
class PreferenceManagementExceptionHandler {
    private val logger = LoggerFactory.getLogger(PreferenceManagementExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<Map<String, Any>> {
        logger.warn("Invalid argument: {}", e.message)
        return ResponseEntity.badRequest()
            .body(
                mapOf(
                    "success" to false,
                    "error" to "Invalid input: ${e.message}",
                    "timestamp" to Instant.now()
                )
            )
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<Map<String, Any>> {
        logger.warn("Invalid state: {}", e.message)
        return ResponseEntity.badRequest()
            .body(
                mapOf(
                    "success" to false,
                    "error" to "Invalid operation: ${e.message}",
                    "timestamp" to Instant.now()
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<Map<String, Any>> {
        logger.error("Unexpected error", e)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                mapOf(
                    "success" to false,
                    "error" to "An unexpected error occurred",
                    "timestamp" to Instant.now()
                )
            )
    }
}