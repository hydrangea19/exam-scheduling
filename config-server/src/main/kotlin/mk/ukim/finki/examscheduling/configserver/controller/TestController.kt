package mk.ukim.finki.examscheduling.configserver.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/test")
class ConfigServerTestController {

    @GetMapping("/ping")
    fun ping(): Map<String, Any> {
        return mapOf(
            "message" to "Configuration Server is running",
            "timestamp" to Instant.now(),
            "service" to "config-server",
            "version" to "1.0.0-SNAPSHOT"
        )
    }
}