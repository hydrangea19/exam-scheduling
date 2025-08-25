package mk.ukim.finki.examscheduling.preferencemanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
@ComponentScan(
    basePackages = [
        "mk.ukim.finki.examscheduling.shared",
        "mk.ukim.finki.examscheduling.preferencemanagement",
        "mk.ukim.finki.examscheduling.sharedsecurity",
    ]
)
class PreferenceManagementApplication

fun main(args: Array<String>) {
    runApplication<PreferenceManagementApplication>(*args)
}