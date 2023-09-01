package api.banking.repository

import api.banking.model.Account
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface AccountRepository: ReactiveCrudRepository<Account, Long>