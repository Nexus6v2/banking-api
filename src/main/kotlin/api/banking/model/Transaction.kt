package api.banking.model

import org.springframework.data.annotation.Id
import java.math.BigDecimal

data class Transaction(
    @Id
    val id: Long?,
    val amount: BigDecimal?,
    val sender: Long?,
    val recipient: Long?,
)