package com.demo.app.movement.repositories;

import com.demo.app.movement.entitites.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Date;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findByIdentifier(String identifier);

    Flux<Transaction> findByIdentifierAndCommissionGreaterThan(String identifier, BigDecimal commission);

    Flux<Transaction> findByCommissionGreaterThan(BigDecimal commission);

    Flux<Transaction> findByIdentifierAndCreateAtBetweenAndCommissionGreaterThan(String identifier, Date startDate, Date finishDate, BigDecimal commission);
}
