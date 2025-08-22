package mk.ukim.finki.examscheduling.schedulingservice.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/test")
class TestController {

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "Scheduling Service is running",
            "timestamp" to Instant.now(),
            "service" to "scheduling-service",
            "version" to "1.0.0-SNAPSHOT"
        )
    }
}