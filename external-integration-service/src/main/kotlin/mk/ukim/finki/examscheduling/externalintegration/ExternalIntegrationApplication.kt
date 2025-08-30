package mk.ukim.finki.examscheduling.externalintegration

import mk.ukim.finki.examscheduling.externalintegration.domain.configproperties.ExternalServicesProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan
import org.springframework.retry.annotation.EnableRetry


@SpringBootApplication
@EnableFeignClients
@EnableRetry
@EnableCaching
@EnableConfigurationProperties(ExternalServicesProperties::class)
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