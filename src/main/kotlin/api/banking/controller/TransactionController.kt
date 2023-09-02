package api.banking.controller

import api.banking.model.ClientException
import api.banking.model.Transaction
import api.banking.model.dto.CreateTransactionRequest
import api.banking.service.TransactionsService
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/transactions")
class TransactionController(val transactionsService: TransactionsService) {

    @PostMapping
    fun createTransaction(transactionRequest: CreateTransactionRequest): Mono<ResponseEntity<Transaction>> {
        return transactionsService.createTransaction(
            transactionRequest.amount,
            transactionRequest.from,
            transactionRequest.to
        )
            .map { ResponseEntity.ok(it) }
            .onErrorResume { e: Throwable ->
                when (e) {
                    is ClientException -> Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
                    is OptimisticLockingFailureException -> Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                    else -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                }
            }
    }
}