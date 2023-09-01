package api.banking.service

import api.banking.model.InsufficientFundsException
import api.banking.model.InvalidAmountException
import api.banking.model.Transaction
import api.banking.repository.TransactionRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class TransactionsService(val transactionRepository: TransactionRepository,
                          val accountsService: AccountsService
) {

    @Transactional
    @Retryable(
        retryFor = [OptimisticLockingFailureException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 500)
    )
    fun createTransaction(amount: BigDecimal,
                          fromId: Long,
                          toId: Long
    ): Mono<Transaction> {
        if (amount <= BigDecimal.ZERO) {
            return Mono.error(InvalidAmountException("Transaction amount must be positive"))
        }

        return accountsService.getAccount(fromId).flatMap { fromAccount ->
            if (amount > fromAccount.balance) {
                Mono.error(InsufficientFundsException("Insufficient funds"))
            } else {
                accountsService.getAccount(toId).flatMap { toAccount ->
                    Mono.zip(
                        accountsService.updateAccountBalance(fromId, fromAccount.balance!! - amount),
                        accountsService.updateAccountBalance(toId, toAccount.balance!! + amount),
                        transactionRepository.save(Transaction(
                            id = null,
                            amount = amount,
                            from = fromId,
                            to = toId
                        ))
                    ).map { it.t3 }
                }
            }
        }.onErrorResume(OptimisticLockingFailureException::class.java) {
            transactionRepository.save(Transaction(
                id = null,
                amount = amount,
                from = fromId,
                to = toId,
                failed = true
            ))
        }
    }
}