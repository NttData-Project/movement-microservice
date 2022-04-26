package com.demo.app.movement.services;

import com.demo.app.movement.entitites.Movement;
import com.demo.app.movement.entitites.TargetAccount;
import com.demo.app.movement.entitites.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface MovementService {
    Flux<Movement> findAll();
    Mono<Movement> saveTransactionOfCurrentAccount(Movement movement,TargetAccount type);
    Mono<Movement> saveTransactionOfSavingAccount(Movement movement,TargetAccount type);
    Mono<Movement> saveTransactionOfFixedTermAccount(Movement movement,TargetAccount type);
    Mono<Movement> findById(String id);
    Mono<Movement> update(Movement movement, String id);
    Mono<Void> delete(String id);
    Flux<Transaction> findByIdentifier(String identifier);
    Mono<BigDecimal> productBalance(String accountNumber);
}
