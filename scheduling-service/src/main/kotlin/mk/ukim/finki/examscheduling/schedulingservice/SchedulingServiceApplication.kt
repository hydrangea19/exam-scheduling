package mk.ukim.finki.examscheduling.schedulingservice

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
        "mk.ukim.finki.examscheduling.schedulingservice",
        "mk.ukim.finki.examscheduling.sharedsecurity",
    ]
)
class SchedulingServiceApplication

fun main(args: Array<String>) {
    runApplication<SchedulingServiceApplication>(*args)
}