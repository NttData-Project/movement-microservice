package com.demo.app.movement.services.impl;

import com.demo.app.movement.entitites.Movement;
import com.demo.app.movement.entitites.TargetAccount;
import com.demo.app.movement.entitites.Transaction;
import com.demo.app.movement.models.CurrentAccount;
import com.demo.app.movement.models.FixedTermAccount;
import com.demo.app.movement.models.SavingAccount;
import com.demo.app.movement.repositories.MovementRepository;
import com.demo.app.movement.repositories.TransactionRepository;
import com.demo.app.movement.services.MovementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

@Service
public class MovementServiceImpl implements MovementService {

    private final MovementRepository movementRepository;
    private final TransactionRepository transactionRepository;
    private final WebClient webClientPassiveCard;
    private final WebClient webClientActiveCard;
    private final WebClient webClientUser;

    public MovementServiceImpl(MovementRepository movementRepository, TransactionRepository transactionRepository, WebClient.Builder webClientPassive, @Value("${passive.card}") String pasiveCardUrl,
                               @Value("${active.card}") String activeCardUrl, WebClient.Builder webClientActive,
                               @Value("${user}") String userUrl, WebClient.Builder webClientUser) {
        this.movementRepository = movementRepository;
        this.transactionRepository = transactionRepository;
        this.webClientActiveCard = webClientActive.baseUrl(activeCardUrl).build();
        this.webClientPassiveCard = webClientPassive.baseUrl("http://localhost:8022").build();
        this.webClientUser = webClientUser.baseUrl(userUrl).build();
    }

    private Mono<CurrentAccount> findCurrentAccountByDni(String dni, String account){
        return webClientPassiveCard.get().uri("/currentAccount/dni/" + dni + "/account/"+account).
                retrieve().bodyToMono(CurrentAccount.class);
    }
    private Mono<SavingAccount> findSavingAccountByDni(String dni, String account){
        return webClientPassiveCard.get().uri("/savingAccount/dni/" + dni + "/account/"+account).
                retrieve().bodyToMono(SavingAccount.class);
    }
    private Mono<FixedTermAccount> findFixedTermAccountByDni(String dni, String account){
        return webClientPassiveCard.get().uri("/fixedTermAccount/dni/" + dni + "/account/"+account).
                retrieve().bodyToMono(FixedTermAccount.class);
    }

    private Mono<CurrentAccount> updateCurrentAccount(CurrentAccount currentAccount){
        return webClientPassiveCard.put().uri("/currentAccount/" + currentAccount.getId()).
                body(Mono.just(currentAccount), CurrentAccount.class)
                .retrieve()
                .bodyToMono(CurrentAccount.class);
    }
    private Mono<SavingAccount> updateSavingAccount(SavingAccount savingAccount){
        return webClientPassiveCard.put().uri("/savingAccount/" + savingAccount.getId()).
                body(Mono.just(savingAccount), SavingAccount.class)
                .retrieve()
                .bodyToMono(SavingAccount.class);
    }
    private Mono<FixedTermAccount> updateFixedTermAccount(FixedTermAccount fixedTermAccount){
        return webClientPassiveCard.put().uri("/fixedTermAccount/" + fixedTermAccount.getId()).
                body(Mono.just(fixedTermAccount), FixedTermAccount.class)
                .retrieve()
                .bodyToMono(FixedTermAccount.class);
    }
    private Transaction createTransaction(Movement movement,Boolean type) {
        Transaction transaction = new Transaction();
        transaction.setMovementId(movement.getId());
        if (type) {
            transaction.setAmount(movement.getAmount().negate());
            transaction.setAccountNumber(movement.getAccountNumber());
            transaction.setDni(movement.getDni());
        } else {
            transaction.setAmount(movement.getAmount());
            transaction.setAccountNumber(movement.getTargetAccount());
            transaction.setDni(movement.getTargetDni());
        }
        transaction.setCurrency(movement.getCurrency());
        transaction.setDescription(movement.getDescription());
        return transaction;
    }
    private Mono<Movement> createMovementAndTransaction(Movement movement){
        return movementRepository.save(movement).flatMap(x->
                Mono.zip(transactionRepository.save(createTransaction(x,false)),transactionRepository.save(createTransaction(x,true))).
                        map(result->{
                            result.getT1();
                            return result.getT2();
                        })).thenReturn(movement);
    }
    private Mono<?> targetAccount(Movement movement,TargetAccount type){
        if(type.equals(TargetAccount.CUENTA_CORRIENTE)) {
            return findCurrentAccountByDni(movement.getTargetDni(), movement.getTargetAccount()).flatMap(x->{
                x.setBalance(x.getBalance().add(movement.getAmount()));
                return updateCurrentAccount(x);
            });
        }
        if(type.equals(TargetAccount.AHORRO)){
            return findSavingAccountByDni(movement.getTargetDni(), movement.getTargetAccount()).flatMap(x->{
                x.setBalance(x.getBalance().add(movement.getAmount()));
                return updateSavingAccount(x);
            });
        }
        else return findFixedTermAccountByDni(movement.getTargetDni(), movement.getTargetAccount()).flatMap(x->{
            if(x.getNumberTransactions().equals(0)) movement.setCommission(BigDecimal.valueOf(4));
            x.setBalance(x.getBalance().add(movement.getAmount().negate()));
            return updateFixedTermAccount(x);
        });
    }
    @Override
    public Flux<Movement> findAll() {
        return movementRepository.findAll();
    }

    @Override
    public Mono<Movement> saveTransactionOfCurrentAccount(Movement movement,TargetAccount type) {
        movement.setCommission(BigDecimal.valueOf(0));
        Mono<CurrentAccount> account= findCurrentAccountByDni(movement.getDni(), movement.getAccountNumber()).flatMap(x->{
            if(x.getCvc().equals(movement.getCvc())){
                x.setBalance(x.getBalance().add(movement.getAmount().negate()));
                return updateCurrentAccount(x);
            }
            return Mono.empty();
        });
        return targetAccount(movement,type).hasElement().flatMap(flag->flag?account.then(createMovementAndTransaction(movement)):Mono.empty());
    }

    @Override
    public Mono<Movement> saveTransactionOfSavingAccount(Movement movement,TargetAccount type) {
        Mono<SavingAccount> account= findSavingAccountByDni(movement.getDni(), movement.getAccountNumber()).flatMap(x->{
            if(x.getCvc().equals(movement.getCvc())) {
                if (x.getNumberTransactions().equals(0)) movement.setCommission(BigDecimal.valueOf(4));
                else x.setNumberTransactions(x.getNumberTransactions() - 1);
                x.setBalance(x.getBalance().add(movement.getAmount().negate()));
                return updateSavingAccount(x);
            }
            return Mono.empty();
        });
        return targetAccount(movement,type).hasElement().flatMap(flag->flag?account.then(createMovementAndTransaction(movement)):Mono.empty());
    }

    @Override
    public Mono<Movement> saveTransactionOfFixedTermAccount(Movement movement,TargetAccount type) {
        SimpleDateFormat dtf = new SimpleDateFormat("dd-MM-yyyy");
        Calendar calendar = Calendar.getInstance();
        Date dateObj = calendar.getTime();
        String formattedDate = dtf.format(dateObj);
        movement.setCommission(BigDecimal.valueOf(0));
        System.out.println(formattedDate);
        Mono<FixedTermAccount> account= findFixedTermAccountByDni(movement.getDni(), movement.getAccountNumber()).flatMap(x->{
            if(x.getCvc().equals(movement.getCvc()) && !x.getNumberTransactions().equals(0) && formattedDate.equals("24/04/2022")) {
                x.setNumberTransactions(x.getNumberTransactions() - 1);
                x.setBalance(x.getBalance().add(movement.getAmount().negate()));
                return updateFixedTermAccount(x);
            }
            return Mono.empty();
        });
        return targetAccount(movement,type).hasElement().flatMap(flag->flag?account.then(createMovementAndTransaction(movement)):Mono.empty());
    }


    @Override
    public Mono<Movement> findById(String id) {
        return movementRepository.findById(id);
    }

    @Override
    public Mono<Movement> update(Movement movement, String id) {
        return movementRepository.findById(id).flatMap(x->{
            x.setAmount(movement.getAmount());
            x.setCurrency(movement.getCurrency());
            x.setAccountNumber(movement.getAccountNumber());
            x.setCvc(movement.getCvc());
            return movementRepository.save(x);
        });
    }

    @Override
    public Mono<Void> delete(String id) {
        return movementRepository.deleteById(id);
    }

}
