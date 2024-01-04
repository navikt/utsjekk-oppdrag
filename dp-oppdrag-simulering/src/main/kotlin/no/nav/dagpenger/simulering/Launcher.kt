package no.nav.dagpenger.simulering

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Launcher

fun main(args: Array<String>) {
    SpringApplication.run(no.nav.dagpenger.simulering.Launcher::class.java, *args)
}
