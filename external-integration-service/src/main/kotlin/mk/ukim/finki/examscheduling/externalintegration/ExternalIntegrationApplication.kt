package mk.ukim.finki.examscheduling.externalintegration

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
//@EnableCaching
class ExternalIntegrationApplication

fun main(args: Array<String>) {
    runApplication<ExternalIntegrationApplication>(*args)
}