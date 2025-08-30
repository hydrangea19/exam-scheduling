package mk.ukim.finki.examscheduling.externalintegration.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfiguration {

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = ConcurrentMapCacheManager()
        cacheManager.setCacheNames(
            listOf(
                "courses",
                "all-courses",
                "professors",
                "all-professors",
                "rooms",
                "all-rooms",
                "enrollments"
            )
        )
        return cacheManager
    }
}