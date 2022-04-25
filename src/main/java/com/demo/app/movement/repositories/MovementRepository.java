package com.demo.app.movement.repositories;

import com.demo.app.movement.entitites.Movement;
import com.demo.app.movement.entitites.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MovementRepository extends ReactiveMongoRepository<Movement,String> {

}
