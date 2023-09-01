package api.banking.model.dto

import java.math.BigDecimal

data class AccountDto(
    val id: Long,
    val balance: BigDecimal
)