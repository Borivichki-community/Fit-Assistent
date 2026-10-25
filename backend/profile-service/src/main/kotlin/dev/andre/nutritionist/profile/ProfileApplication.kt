package dev.andre.nutritionist.profile

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProfileApplication

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<ProfileApplication>(*args)
}
