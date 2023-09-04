package api.banking

import api.banking.model.Account
import api.banking.model.Transaction
import api.banking.model.dto.AccountDto
import api.banking.model.dto.CreateAccountRequest
import api.banking.model.dto.CreateTransactionRequest
import api.banking.repository.AccountRepository
import api.banking.repository.TransactionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.PostgreSQLContainer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Duration
import java.util.ArrayList


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BankingApiApplicationTests {

	@Autowired
	lateinit var transactionRepository: TransactionRepository

	@Autowired
	lateinit var accountRepository: AccountRepository

	@Autowired
	lateinit var testRestTemplate: TestRestTemplate

	@LocalServerPort
	var localServerPort: Int = 0

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
		val amount = BigDecimal.valueOf(5.555)

		val request = CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)
		val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)

		assertEquals(HttpStatus.OK, response.statusCode)
		val responseBody = response.body!!
		val createdTransaction = transactionRepository.findById(responseBody.id!!).block()!!
		assertEquals(amount, createdTransaction.amount)
		assertEquals(senderId, createdTransaction.sender)
		assertEquals(recipientId, createdTransaction.recipient)
		val updatedSender = accountRepository.findById(senderId).block()!!
		assertEquals(balance - amount, updatedSender.balance)
		val updatedRecipient = accountRepository.findById(recipientId).block()!!
		assertEquals(balance + amount, updatedRecipient.balance)
	}

	@Test
	fun testTransactionCreateNegativeAmount() {
		val balance = BigDecimal.valueOf(100.2)
		val senderAccount = createAccount(balance = balance)
		val senderId = senderAccount.id!!
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount = BigDecimal.valueOf(-100.2)

		val request = CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)
		val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		val sender = accountRepository.findById(senderId).block()!!
		assertEquals(balance, sender.balance)
		val recipient = accountRepository.findById(recipientId).block()!!
		assertEquals(balance, recipient.balance)
	}

	@Test
	fun testTransactionCreateZeroAmount() {
		val balance = BigDecimal.valueOf(100.2)
		val senderAccount = createAccount(balance = balance)
		val senderId = senderAccount.id!!
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount = BigDecimal.valueOf(0)

		val request = CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)
		val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		val sender = accountRepository.findById(senderId).block()!!
		assertEquals(balance, sender.balance)
		val recipient = accountRepository.findById(recipientId).block()!!
		assertEquals(balance, recipient.balance)
	}

	@Test
	fun testTransactionCreateAsync() {
		val balance = BigDecimal.valueOf(100.11)
		val senderAccount = createAccount(balance = balance)
		val senderId = senderAccount.id!!
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount = BigDecimal.valueOf(0.1)

		val numberOfOperations = 20
		val client = WebClient.create("http://localhost:$localServerPort")
		Flux.range(1, numberOfOperations)
			.delayElements(Duration.ofMillis(200))
			.map { Mono.just(CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)) }
			.flatMap {
				client.post()
					.uri("/transactions")
					.contentType(MediaType.APPLICATION_JSON)
					.body(it, CreateTransactionRequest::class.java)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toEntity(Transaction::class.java)
			}
			.onErrorResume { Mono.empty<ResponseEntity<Transaction>>() }
			.collectList()
			.block()

		val updatedSender = accountRepository.findById(senderId).block()!!
		assertEquals(balance - amount * numberOfOperations.toBigDecimal(), updatedSender.balance)
		val updatedRecipient = accountRepository.findById(recipientId).block()!!
		assertEquals(balance + amount * numberOfOperations.toBigDecimal(), updatedRecipient.balance)
	}

	@Test
	fun testTransactionCreateConcurrent() {
		val balance = BigDecimal.valueOf(100.11)
		val senderAccount = createAccount(balance = balance)
		val senderId = senderAccount.id!!
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount = BigDecimal.valueOf(0.1)

		val numberOfOperations = 200
		val successfulResponses = makeTransactionRequests(numberOfOperations, amount, senderId, recipientId)

		val updatedSender = accountRepository.findById(senderId).block()!!
		assertEquals(balance - amount * successfulResponses.size.toBigDecimal(), updatedSender.balance)
		val updatedRecipient = accountRepository.findById(recipientId).block()!!
		assertEquals(balance + amount * successfulResponses.size.toBigDecimal(), updatedRecipient.balance)
	}

	@Test
	fun testTransactionCreateInsufficientFunds() {
		val balance = BigDecimal.valueOf(0.5)
		val senderAccount = createAccount(balance = balance)
		val senderId = senderAccount.id!!
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount = BigDecimal.valueOf(0.1)

		val numberOfOperations = 10
		val successfulResponses = makeTransactionRequests(numberOfOperations, amount, senderId, recipientId)
		val updatedSender = accountRepository.findById(senderId).block()!!
		val updatedSenderBalance = balance - amount * successfulResponses.size.toBigDecimal()
		assertEquals(updatedSenderBalance, updatedSender.balance)
		assertEquals(0.0.toBigDecimal(), updatedSender.balance)
		val updatedRecipient = accountRepository.findById(recipientId).block()!!
		val updatedRecipientBalance = balance + amount * successfulResponses.size.toBigDecimal()
		assertEquals(updatedRecipientBalance, updatedRecipient.balance)

		val request = CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)
		val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
		val newUpdatedSender = accountRepository.findById(senderId).block()!!
		assertEquals(updatedSenderBalance, newUpdatedSender.balance)
		val newUpdatedRecipient = accountRepository.findById(recipientId).block()!!
		assertEquals(updatedRecipientBalance, newUpdatedRecipient.balance)
	}

	@Test
	fun testTransactionCreateInvalidSenderAccount() {
		val balance = BigDecimal.valueOf(100.2)
		val senderId = 100500L
		val recipientAccount = createAccount(balance = balance)
		val recipientId = recipientAccount.id!!
		val amount = BigDecimal.valueOf(1.01)

		val request = CreateTransactionRequest(amount = amount, from = senderId, to = recipientId)
		val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)

		assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
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

	private fun makeTransactionRequests(requestAmount: Int = 20, transactionAmount: BigDecimal, senderId: Long, recipientId: Long): ArrayList<ResponseEntity<Transaction>> {
		val successfulResponses = ArrayList<ResponseEntity<Transaction>>()

		runBlocking {
			repeat(requestAmount) {
				launch {
					val request = CreateTransactionRequest(amount = transactionAmount, from = senderId, to = recipientId)
					val response = testRestTemplate.postForEntity("/transactions", request, Transaction::class.java)
					if (response.statusCode == HttpStatus.OK) successfulResponses.add(response)
				}
			}
		}

		return successfulResponses
	}

	private fun createAccount(balance: BigDecimal = BigDecimal.valueOf(10)): Account {
		return accountRepository.save(Account(
			id = null,
			balance = balance,
			version = 0
		)).block()!!
	}
}
