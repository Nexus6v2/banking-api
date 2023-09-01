package api.banking.repository

import api.banking.model.Transaction
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface TransactionRepository: ReactiveCrudRepository<Transaction, Long>