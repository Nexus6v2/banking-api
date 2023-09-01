package api.banking.model

import org.springframework.data.annotation.Id
import java.math.BigDecimal

class Transaction(
    @Id
    val id: Long?,
    val amount: BigDecimal?,
    val from: Long?,
    val to: Long?,
    var failed: Boolean = false
)