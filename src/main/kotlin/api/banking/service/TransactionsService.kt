package api.banking.service

import api.banking.model.InsufficientFundsException
import api.banking.model.InvalidAmountException
import api.banking.model.Transaction
import api.banking.repository.TransactionRepository
import io.r2dbc.spi.ConnectionFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.math.BigDecimal


@Service
class TransactionsService(val transactionRepository: TransactionRepository,
                          val accountsService: AccountsService
) {

    @Transactional
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
                    accountsService.updateAccountBalance(fromId, fromAccount.balance!! - amount)
                        .then(accountsService.updateAccountBalance(toId, toAccount.balance!! + amount))
                        .then(transactionRepository.save(Transaction(
                            id = null,
                            amount = amount,
                            sender = fromId,
                            recipient = toId
                        )))
                }
            }
        }
    }
}