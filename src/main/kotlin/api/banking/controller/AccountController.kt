package api.banking.controller

import api.banking.model.ClientException
import api.banking.model.dto.AccountDto
import api.banking.model.dto.CreateAccountRequest
import api.banking.service.AccountsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.math.BigDecimal

@RestController
@RequestMapping("/accounts")
class AccountController(val accountsService: AccountsService) {

    @GetMapping("/{id}/balance")
    fun getBalance(@PathVariable id: Long): Mono<ResponseEntity<BigDecimal>> {
        return accountsService.getAccountBalance(id)
            .map { ResponseEntity.ok(it) }
            .onErrorResume(ClientException::class.java) {
                Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
            }
    }

    @PostMapping
    fun create(@RequestBody createAccountRequest: CreateAccountRequest): Mono<ResponseEntity<AccountDto>> {
        return accountsService.createAccount(createAccountRequest.balance)
            .map { AccountDto(it.id!!, it.balance!!) }
            .map { ResponseEntity.ok(it) }
            .onErrorResume(ClientException::class.java) {
                Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
            }
    }
}