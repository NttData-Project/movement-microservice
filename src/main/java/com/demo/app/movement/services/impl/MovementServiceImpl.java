package com.demo.app.movement.services.impl;

import com.demo.app.movement.entitites.Movement;
import com.demo.app.movement.entitites.TargetAccount;
import com.demo.app.movement.entitites.Transaction;
import com.demo.app.movement.models.CreditAccount;
import com.demo.app.movement.models.CurrentAccount;
import com.demo.app.movement.models.FixedTermAccount;
import com.demo.app.movement.models.SavingAccount;
import com.demo.app.movement.repositories.MovementRepository;
import com.demo.app.movement.repositories.TransactionRepository;
import com.demo.app.movement.services.MovementService;
import com.demo.app.movement.utils.DateProcess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
        this.webClientPassiveCard = webClientPassive.baseUrl(pasiveCardUrl).build();
        this.webClientUser = webClientUser.baseUrl(userUrl).build();
    }

    private Mono<CurrentAccount> findCurrentAccountByIdentifier(String id) {
        return webClientPassiveCard.get().uri("/currentAccount/" + id).
                retrieve().bodyToMono(CurrentAccount.class);
    }

    private Mono<SavingAccount> findSavingAccountByIdentifier(String id) {
        return webClientPassiveCard.get().uri("/savingAccount/" + id).
                retrieve().bodyToMono(SavingAccount.class);
    }

    private Mono<FixedTermAccount> findFixedTermAccountByIdentifier(String id) {
        return webClientPassiveCard.get().uri("/fixedTermAccount/" + id).
                retrieve().bodyToMono(FixedTermAccount.class);
    }

    private Mono<CurrentAccount> updateCurrentAccount(CurrentAccount currentAccount) {
        return webClientPassiveCard.put().uri("/currentAccount/" + currentAccount.getId()).
                body(Mono.just(currentAccount), CurrentAccount.class)
                .retrieve()
                .bodyToMono(CurrentAccount.class);
    }

    private Mono<CreditAccount> findCreditAccountByIdentifier(String identifier){
        return webClientActiveCard.get().uri("/idCreditAccount/" + identifier)
                .retrieve().bodyToMono(CreditAccount.class);
    }

    private Mono<SavingAccount> updateSavingAccount(SavingAccount savingAccount) {
        return webClientPassiveCard.put().uri("/savingAccount/" + savingAccount.getId()).
                body(Mono.just(savingAccount), SavingAccount.class)
                .retrieve()
                .bodyToMono(SavingAccount.class);
    }

    private Mono<FixedTermAccount> updateFixedTermAccount(FixedTermAccount fixedTermAccount) {
        return webClientPassiveCard.put().uri("/fixedTermAccount/" + fixedTermAccount.getId()).
                body(Mono.just(fixedTermAccount), FixedTermAccount.class)
                .retrieve()
                .bodyToMono(FixedTermAccount.class);
    }

    private Transaction createTransaction(Movement movement, Boolean type) {
        Transaction transaction = new Transaction();
        transaction.setMovementId(movement.getId());
        transaction.setAmount(movement.getAmount());
        if (type) {
            transaction.setCommission(movement.getCommission());
            transaction.setIdentifier(movement.getIdentifier());
        } else {
            transaction.setIdentifier(movement.getTargetIdentifier());
            transaction.setCommission(BigDecimal.valueOf(0));
        }
        transaction.setType(type);
        transaction.setCurrency(movement.getCurrency());
        transaction.setDescription(movement.getDescription());
        return transaction;
    }

    private Mono<Movement> createMovementAndTransaction(Movement movement) {
        return movementRepository.save(movement).flatMap(x ->
                Mono.zip(transactionRepository.save(createTransaction(x, false)), transactionRepository.save(createTransaction(x, true))).
                        map(result -> {
                            result.getT1();
                            return result.getT2();
                        })).thenReturn(movement);
    }

    private Mono<?> targetAccount(Movement movement, TargetAccount type) {
        if (type.equals(TargetAccount.CUENTA_CORRIENTE)) {
            return findCurrentAccountByIdentifier(movement.getTargetIdentifier()).flatMap(x -> {
                x.setBalance(x.getBalance().add(movement.getAmount()));
                return updateCurrentAccount(x);
            });
        }
        if (type.equals(TargetAccount.AHORRO)) {
            return findSavingAccountByIdentifier(movement.getTargetIdentifier()).flatMap(x -> {
                x.setBalance(x.getBalance().add(movement.getAmount()));
                return updateSavingAccount(x);
            });
        } else return findFixedTermAccountByIdentifier(movement.getTargetIdentifier()).flatMap(x -> {
            x.setBalance(x.getBalance().add(movement.getAmount()));
            return updateFixedTermAccount(x);
        });
    }

    @Override
    public Flux<Movement> findAll() {
        return movementRepository.findAll();
    }

    @Override
    public Mono<Movement> saveTransactionOfCurrentAccount(Movement movement, TargetAccount type) {
        movement.setCommission(BigDecimal.valueOf(0));
        Mono<CurrentAccount> account = findCurrentAccountByIdentifier(movement.getIdentifier()).flatMap(x -> {
            if (x.getCvc().equals(movement.getCvc())) {
                x.setBalance(x.getBalance().add(movement.getAmount().negate()));
                return updateCurrentAccount(x);
            }
            return Mono.empty();
        });
        return account.hasElement().flatMap(flag -> flag ? targetAccount(movement, type).then(createMovementAndTransaction(movement)) : Mono.empty());
    }

    @Override
    public Mono<Movement> saveTransactionOfSavingAccount(Movement movement, TargetAccount type) {
        Mono<SavingAccount> account = findSavingAccountByIdentifier(movement.getIdentifier()).flatMap(x -> {
            if (x.getCvc().equals(movement.getCvc())) {
                if (x.getNumberTransactions().equals(0)) movement.setCommission(BigDecimal.valueOf(4));
                else x.setNumberTransactions(x.getNumberTransactions() - 1);
                x.setBalance(x.getBalance().add(movement.getAmount().negate()));
                return updateSavingAccount(x);
            }
            return Mono.empty();
        });
        return account.hasElement().flatMap(flag -> flag ? targetAccount(movement, type).then(createMovementAndTransaction(movement)) : Mono.empty());
    }

    @Override
    public Mono<Movement> saveTransactionOfFixedTermAccount(Movement movement, TargetAccount type) {
        SimpleDateFormat dtf = new SimpleDateFormat("dd-MM-yyyy");
        Calendar calendar = Calendar.getInstance();
        Date dateObj = calendar.getTime();
        String formattedDate = dtf.format(dateObj);
        movement.setCommission(BigDecimal.valueOf(0));
        System.out.println(formattedDate);
        Mono<FixedTermAccount> account = findFixedTermAccountByIdentifier(movement.getIdentifier()).flatMap(x -> {
            if (x.getCvc().equals(movement.getCvc()) && !x.getNumberTransactions().equals(0) && formattedDate.equals("24/04/2022")) {
                x.setNumberTransactions(x.getNumberTransactions() - 1);
                x.setBalance(x.getBalance().add(movement.getAmount().negate()));
                return updateFixedTermAccount(x);
            }
            return Mono.empty();
        });
        return account.hasElement().flatMap(flag -> flag ? targetAccount(movement, type).then(createMovementAndTransaction(movement)) : Mono.empty());
    }


    @Override
    public Mono<Movement> findById(String id) {
        return movementRepository.findById(id);
    }

    @Override
    public Mono<Movement> update(Movement movement, String id) {
        return movementRepository.findById(id).flatMap(x -> {
            x.setAmount(movement.getAmount());
            x.setCurrency(movement.getCurrency());
            x.setCvc(movement.getCvc());
            return movementRepository.save(x);
        });
    }

    @Override
    public Mono<Void> delete(String id) {
        return movementRepository.deleteById(id);
    }

    @Override
    public Flux<Transaction> findByIdentifier(String identifier) {
        return transactionRepository.findByIdentifier(identifier);
    }

    @Override
    public Mono<BigDecimal> productBalance(String identifier) {
        Mono<BigDecimal> balance = transactionRepository.findByIdentifier(identifier)
                .map(item -> item.getType() == false ? item.getAmount() : item.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance;
    }

    @Override
    public Flux<Transaction> findByIdentifierAndCreateAtBetweenAndCommissionGreaterThan(String identifier, Date startDate, Date finishDate, BigDecimal commission) {
        return transactionRepository.findByIdentifierAndCreateAtBetweenAndCommissionGreaterThan(identifier, startDate, finishDate, commission);
    }

    @Override
    public Mono<BigDecimal> comisionBalanceByRange(String identifier, Date startDate, Date finishDate, BigDecimal commission) {
        Mono<BigDecimal> balance = transactionRepository.findByIdentifier(identifier)
                .map(tp -> tp.getType() == true ? tp.getAmount() : new BigDecimal(0))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return balance;
    }

    @Override
    public Mono<BigDecimal> productBalanceByPeriod(String identifier) {
        Calendar today = Calendar.getInstance();
        Calendar cutDate = Calendar.getInstance();
        Mono<BigDecimal> valor = findCreditAccountByIdentifier(identifier)
                .flatMap(cc -> {
                    Boolean result = false;
                    cutDate.setTime(cc.getCutoffDate());
                    result = today.before(cutDate);
                    return !result ? transactionRepository.findByIdentifierAndCreateAtBetween(identifier, DateProcess.reduceOneMonth(DateProcess.updateDate(cc.getCutoffDate(), 0), -1), DateProcess.reduceOneMonth(DateProcess.updateDate(cc.getCutoffDate(), 1), 0))
                            .map(tm ->
                                    !tm.getType() ? tm.getAmount() : tm.getAmount().negate())
                            .reduce(BigDecimal.ZERO, BigDecimal::add) : transactionRepository.findByIdentifierAndCreateAtBetween(identifier, DateProcess.reduceOneMonth(DateProcess.updateDate(cc.getCutoffDate(), 0), -2), DateProcess.reduceOneMonth(DateProcess.updateDate(cc.getCutoffDate(), 1), -1)).map(tm ->
                                    !tm.getType() ? tm.getAmount() : tm.getAmount().negate())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                });

        Mono<BigDecimal> paymentsOutOfCutPeriod = findCreditAccountByIdentifier(identifier)
                .flatMap(cc -> {
                    Boolean result = false;
                    cutDate.setTime(cc.getCutoffDate());
                    result = today.before(cutDate);
                    return !result ? transactionRepository.findByIdentifierAndTypeAndCreateAtBetween(identifier, false, DateProcess.updateDate(cc.getCutoffDate(), 0), DateProcess.updateDate(cc.getPaymentDate(), 1))
                            .map(tm ->
                                    !tm.getType() ? tm.getAmount() : new BigDecimal(0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add) : transactionRepository.findByIdentifierAndTypeAndCreateAtBetween(identifier, false, DateProcess.reduceOneMonth(DateProcess.updateDate(cc.getCutoffDate(), 0), -1), DateProcess.reduceOneMonth(DateProcess.updateDate(cc.getPaymentDate(), 1), -1)).map(tm ->
                                    !tm.getType() ? tm.getAmount() : new BigDecimal(0))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                });

        return valor.map(c -> {
            paymentsOutOfCutPeriod.map(c::add);
            return c;
        });
    }
}
