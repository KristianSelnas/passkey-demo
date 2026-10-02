package com.kantega.passkeydemo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock

@SpringBootApplication
class PasskeyDemoApplication {

    // Injected where the current time matters, so tests can control it
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<PasskeyDemoApplication>(*args)
}
