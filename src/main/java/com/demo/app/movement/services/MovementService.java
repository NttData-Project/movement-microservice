package com.demo.app.movement.services;

import com.demo.app.movement.entitites.Movement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MovementService {
    Flux<Movement> findAll();
    Mono<Movement> saveTransactionOfCurrentAccount(Movement movement);
    Mono<Movement> saveTransactionOfSavingAccount(Movement movement);
    Mono<Movement> saveTransactionOfFixedTermAccount(Movement movement);
    Mono<Movement> findById(String id);
    Mono<Movement> update(Movement movement, String id);
    Mono<Void> delete(String id);
}
