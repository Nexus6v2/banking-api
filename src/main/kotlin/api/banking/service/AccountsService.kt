package api.banking.service

import api.banking.model.Account
import api.banking.model.AccountNotFoundException
import api.banking.model.InvalidBalanceException
import api.banking.repository.AccountRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal

@Service
class AccountsService(val accountRepository: AccountRepository) {
    val defaultStartingBalance: BigDecimal = BigDecimal.ONE

    fun getAccount(id: Long): Mono<Account> {
        return accountRepository.findById(id)
            .switchIfEmpty(Mono.error(AccountNotFoundException("Account not found")))
    }

    fun getAccountBalance(id: Long): Mono<BigDecimal> {
        return getAccount(id).mapNotNull { it.balance }
    }

    fun updateAccountBalance(id: Long, newBalance: BigDecimal): Mono<Account> {
        if (newBalance < BigDecimal.ZERO) {
            return Mono.error(InvalidBalanceException("Balance must not be negative"))
        }
        return getAccount(id).flatMap {
            it.balance = newBalance
            accountRepository.save(it)
        }
    }

    fun createAccount(startingBalance: BigDecimal?): Mono<Account> {
        val balance = startingBalance ?: defaultStartingBalance
        if (balance <= BigDecimal.ZERO) {
            return Mono.error(InvalidBalanceException("Account starting balance cannot be negative"))
        }
        return accountRepository.save(Account(
            id = null,
            balance = balance,
            version = 0
        ))
    }
}