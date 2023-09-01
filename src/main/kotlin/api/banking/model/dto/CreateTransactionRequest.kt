package api.banking.model.dto

import java.math.BigDecimal

data class CreateTransactionRequest(
    val amount: BigDecimal,
    val from: Long,
    val to: Long
)