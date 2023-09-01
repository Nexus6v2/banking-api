package api.banking.model

open class ClientException(override val message: String?): IllegalStateException()

class InsufficientFundsException(override val message: String?): ClientException(message)
class AccountNotFoundException(override val message: String?): ClientException(message)
class InvalidBalanceException(override val message: String?): ClientException(message)
class InvalidAmountException(override val message: String?): ClientException(message)