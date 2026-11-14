package dev.andre.nutritionist.profile

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class ProfileApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<ProfileApplication>(*args)
}
