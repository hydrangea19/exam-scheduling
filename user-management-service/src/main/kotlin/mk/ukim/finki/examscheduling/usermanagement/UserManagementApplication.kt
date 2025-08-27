package mk.ukim.finki.examscheduling.usermanagement

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan


@SpringBootApplication
//@EnableFeignClients
@ComponentScan(
    basePackages = [
        "mk.ukim.finki.examscheduling.shared",
        "mk.ukim.finki.examscheduling.usermanagement",
        "mk.ukim.finki.examscheduling.sharedsecurity",
    ]
)
class UserManagementApplication

fun main(args: Array<String>) {
    runApplication<UserManagementApplication>(*args)
}