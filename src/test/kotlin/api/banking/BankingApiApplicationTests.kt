package api.banking

import api.banking.model.Account
import api.banking.model.Transaction
import api.banking.model.dto.AccountDto
import api.banking.model.dto.CreateAccountRequest
import api.banking.model.dto.CreateTransactionRequest
import api.banking.repository.AccountRepository
import api.banking.repository.TransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BankingApiApplicationTests {

	@Autowired
	lateinit var transactionRepository: TransactionRepository

	@Autowired
	lateinit var accountRepository: AccountRepository

	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@Test
	fun testAccountCreateInvalidBalance() {
		val requestBody = CreateAccountRequest(balance = BigDecimal.valueOf(-5))

		val response = testRestTemplate.postForEntity("/accounts", requestBody, AccountDto::class.java)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
	}

	@Test
	fun testAccountCreateDefaultBalance() {
		val requestBody = CreateAccountRequest(balance = null)

		val response = testRestTemplate
			.postForEntity("/accounts", requestBody, AccountDto::class.java)

		assertEquals(HttpStatus.OK, response.statusCode)
		val createdAccount = accountRepository.findById(response.body!!.id).block()!!
		assertEquals(BigDecimal.ONE, createdAccount.balance)
	}

	@Test
	fun testAccountCreate() {
		val startingBalance = BigDecimal.valueOf(100)
		val requestBody = CreateAccountRequest(balance = startingBalance)

		val response = testRestTemplate.postForEntity("/accounts", requestBody, AccountDto::class.java)

		assertEquals(HttpStatus.OK, response.statusCode)
		val createdAccount = accountRepository.findById(response.body!!.id).block()!!
		assertEquals(startingBalance, createdAccount.balance)
	}

	@Test
	fun testAccountBalanceGetNotExistingAccount() {
		val accountId = 100500

		val response = testRestTemplate.getForEntity("/accounts/$accountId/balance", BigDecimal::class.java)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
	}

	@Test
	fun testAccountBalanceGet() {
		val balance = BigDecimal.valueOf(100)
		val account = createAccount(balance = balance)

		val response = testRestTemplate.getForEntity("/accounts/${account.id!!}/balance", BigDecimal::class.java)

		assertEquals(HttpStatus.OK, response.statusCode)
		assertEquals(balance, response.body!!)
	}

	@Test
	fun testTransactionCreate() {
		val balance = BigDecimal.valueOf(100.2)
		val senderAccount = createAccount(balance = balance)
		val senderId = senderAccount.id!!
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount: BigDecimal = BigDecimal.valueOf(5.555)

		val request = CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)
		val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)

		assertEquals(HttpStatus.OK, response.statusCode)
		val responseBody = response.body!!
		val createdTransaction = transactionRepository.findById(responseBody.id!!).block()!!
		assertEquals(amount, createdTransaction.amount)
		assertEquals(senderId, createdTransaction.from)
		assertEquals(recipientId, createdTransaction.to)
		val updatedSender = accountRepository.findById(senderId).block()!!
		assertEquals(balance - amount, updatedSender.balance)
		val updatedRecipient = accountRepository.findById(recipientId).block()!!
		assertEquals(balance + amount, updatedRecipient.balance)
	}

	companion object {
		private val postgresContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:15")
			.withUsername("test")
			.withPassword("test")
			.withReuse(true)
			.withExposedPorts(5432)

		@BeforeAll
		@JvmStatic
		fun setup() {
			postgresContainer.start()
		}

		@JvmStatic
		@DynamicPropertySource
		fun databaseProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.r2dbc.url") { postgresContainer.jdbcUrl.replace("jdbc", "r2dbc") }
			registry.add("spring.r2dbc.username") { postgresContainer.username }
			registry.add("spring.r2dbc.password") { postgresContainer.password }

			registry.add("spring.flyway.url", postgresContainer::getJdbcUrl)
			registry.add("spring.flyway.user") { postgresContainer.username }
			registry.add("spring.flyway.password") { postgresContainer.password }
		}
	}

	private fun createAccount(balance: BigDecimal = BigDecimal.valueOf(10)): Account {
		return accountRepository.save(Account(
			id = null,
			balance = balance,
			version = 0
		)).block()!!
	}
}
