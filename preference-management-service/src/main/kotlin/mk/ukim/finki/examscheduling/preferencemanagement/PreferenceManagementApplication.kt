package mk.ukim.finki.examscheduling.preferencemanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
//@EnableFeignClients
//@EnableScheduling
class PreferenceManagementApplication

fun main(args: Array<String>) {
    runApplication<PreferenceManagementApplication>(*args)
}