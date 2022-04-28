package com.demo.app.movement.controllers;

import com.demo.app.movement.entitites.Movement;
import com.demo.app.movement.entitites.TargetAccount;
import com.demo.app.movement.entitites.Transaction;
import com.demo.app.movement.models.CreditAccount;
import com.demo.app.movement.services.MovementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

@RestController
@RequestMapping("/movement")
@Tag(name = "Test APIs", description = "Test APIs for demo purpose")
public class MovementController {
    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public ResponseEntity<Flux<Movement>> findAll() {
        return ResponseEntity.ok(movementService.findAll());
    }

    @GetMapping("/{id}")
    public Mono<Movement> findById(@PathVariable String id) {
        return movementService.findById(id);
    }

    @PostMapping("/currentAccount/{type}")
    public ResponseEntity<Mono<Movement>> saveTransactionByCurrentAccount(@RequestBody Movement movement, @PathVariable TargetAccount type) {
        return ResponseEntity.ok(movementService.saveTransactionOfCurrentAccount(movement, type));
    }

    @PostMapping("/savingAccount/{type}")
    public ResponseEntity<Mono<Movement>> saveTransactionBySavingAccount(@RequestBody Movement movement, @PathVariable TargetAccount type) {
        return ResponseEntity.ok(movementService.saveTransactionOfSavingAccount(movement, type));
    }

    @PostMapping("/fixedTermAccount/{type}")
    public ResponseEntity<Mono<Movement>> saveTransactionByFixedTermAccount(@RequestBody Movement movement, @PathVariable TargetAccount type) {
        return ResponseEntity.ok(movementService.saveTransactionOfFixedTermAccount(movement, type));
    }

    @PostMapping("/creditAccount")
    public ResponseEntity<Mono<Movement>> saveTransactionByCreditAccount(@RequestBody Movement movement) {
        return ResponseEntity.ok(movementService.saveTransactionOfCreditAccount(movement));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Movement>> update(@RequestBody Movement movement, @PathVariable String id) {
        return movementService.update(movement, id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return movementService.delete(id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/balance/{identifier}")
    public Mono<BigDecimal> getBalance(@PathVariable String identifier) {
        return movementService.productBalance(identifier);
    }

    @GetMapping("/movement/{identifier}")
    public Flux<Transaction> getMovementsByIdentifier(@PathVariable String identifier) {
        return movementService.findByIdentifier(identifier);
    }

    @GetMapping("/comission/{identifier}/{startDate}/{finishDate}/{commission}")
    public Flux<Transaction> findByIdentifierAndCreateAtBetweenAndCommissionGreaterThan(@PathVariable String identifier, @PathVariable @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
                                                                                        @PathVariable @DateTimeFormat(pattern = "dd-MM-yyyy") Date finishDate, @PathVariable BigDecimal commission) {
        return movementService.findByIdentifierAndCreateAtBetweenAndCommissionGreaterThan(identifier, startDate, finishDate, commission);
    }

    @GetMapping("/balancecomission/{identifier}/{startDate}/{finishDate}/{commission}")
    public Mono<BigDecimal> comisionBalanceByRange(String identifier, @PathVariable @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate, @PathVariable @DateTimeFormat(pattern = "dd-MM-yyyy") Date finishDate, BigDecimal commission) {
        return movementService.comisionBalanceByRange(identifier, startDate, finishDate, commission);
    }

    @GetMapping("/idCreditAccount/{identifier}")
    public Mono<BigDecimal> productBalanceByPeriod(@PathVariable String identifier) {
        return movementService.productBalanceByPeriod(identifier);
    }
}