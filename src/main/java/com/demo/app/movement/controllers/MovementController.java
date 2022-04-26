package com.demo.app.movement.controllers;

import com.demo.app.movement.entitites.Movement;
import com.demo.app.movement.entitites.TargetAccount;
import com.demo.app.movement.services.MovementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

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

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Movement>> update(@RequestBody Movement movement, @PathVariable String id) {
        return movementService.update(movement, id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return movementService.delete(id).map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/identifier/{identifier}")
    public Mono<BigDecimal> getBalance(@PathVariable String identifier) {
        return movementService.productBalance(identifier);
    }

}
