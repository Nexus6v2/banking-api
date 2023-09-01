package api.banking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BankingApiApplication

fun main(args: Array<String>) {
	runApplication<BankingApiApplication>(*args)
}
