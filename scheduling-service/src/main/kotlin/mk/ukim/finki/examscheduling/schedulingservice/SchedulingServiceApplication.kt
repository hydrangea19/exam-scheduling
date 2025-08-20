package mk.ukim.finki.examscheduling.schedulingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
//@EnableAsync
class SchedulingServiceApplication

fun main(args: Array<String>) {
    runApplication<SchedulingServiceApplication>(*args)
}