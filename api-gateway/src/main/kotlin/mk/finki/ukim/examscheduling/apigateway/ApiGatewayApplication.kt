package mk.finki.ukim.examscheduling.apigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(
    basePackages = [
        "mk.ukim.finki.examscheduling.shared",
        "mk.finki.ukim.examscheduling.apigateway",
        "mk.ukim.finki.examscheduling.sharedsecurity",
    ]
)
class ApiGatewayApplication

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}