package mk.ukim.finki.examscheduling.externalintegration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan


@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
//@EnableCaching
@ComponentScan(
    basePackages = [
        "mk.ukim.finki.examscheduling.sharedsecurity",
        "mk.ukim.finki.examscheduling.externalintegration",
        "mk.ukim.finki.examscheduling.shared",
    ]
)
class ExternalIntegrationApplication

fun main(args: Array<String>) {
    runApplication<ExternalIntegrationApplication>(*args)
}