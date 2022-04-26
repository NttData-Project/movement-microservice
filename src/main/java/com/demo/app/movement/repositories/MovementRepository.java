package com.demo.app.movement.repositories;

import com.demo.app.movement.entitites.Movement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovementRepository extends ReactiveMongoRepository<Movement,String> {
}
