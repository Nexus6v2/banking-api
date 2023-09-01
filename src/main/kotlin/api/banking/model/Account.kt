package api.banking.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import java.math.BigDecimal

data class Account(
    @Id
    val id: Long?,
    var balance: BigDecimal?,
    @Version
    val version: Int = 0
)