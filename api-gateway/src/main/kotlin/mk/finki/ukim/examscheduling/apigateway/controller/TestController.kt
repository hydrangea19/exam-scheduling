package mk.finki.ukim.examscheduling.apigateway.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api/gateway")
class ApiGatewayTestController {

    @GetMapping("/ping")
    fun ping(): Mono<Map<String, Any>> {
        return Mono.just(
            mapOf(
                "message" to "API Gateway is running",
                "timestamp" to Instant.now(),
                "service" to "api-gateway",
                "version" to "1.0.0-SNAPSHOT"
            )
        )
    }
}