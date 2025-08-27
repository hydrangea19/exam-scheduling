package mk.ukim.finki.examscheduling.publishingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
//@EnableAsync
@ComponentScan(
    basePackages = [
        "mk.ukim.finki.examscheduling.shared",
        "mk.ukim.finki.examscheduling.publishingservice",
        "mk.ukim.finki.examscheduling.sharedsecurity",
    ]
)
class PublishingServiceApplication

fun main(args: Array<String>) {
    runApplication<PublishingServiceApplication>(*args)
}