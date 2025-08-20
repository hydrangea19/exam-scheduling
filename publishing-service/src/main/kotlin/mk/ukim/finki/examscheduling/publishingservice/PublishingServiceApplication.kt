package mk.ukim.finki.examscheduling.publishingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
//@EnableAsync
class PublishingServiceApplication

fun main(args: Array<String>) {
    runApplication<PublishingServiceApplication>(*args)
}