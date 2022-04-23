package com.demo.app.movement.services.impl;

import com.demo.app.movement.entitites.Transaction;
import com.demo.app.movement.models.CurrentAccount;
import com.demo.app.movement.models.FixedTermAccount;
import com.demo.app.movement.models.SavingAccount;
import com.demo.app.movement.repositories.TransactionRepository;
import com.demo.app.movement.services.TransactionService;
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
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    private final WebClient webClientPasiveCard;
    private final WebClient webClientActiveCard;
    private final WebClient webClientUser;

    public TransactionServiceImpl(TransactionRepository transactionRepository, WebClient.Builder webClientPasiveCard,@Value("${pasive.card}") String pasiveCardUrl,
                                  @Value("${active.card}") String activeCardUrl,WebClient.Builder webClientActiveCard,
                                  @Value("${user}") String userUrl,WebClient.Builder webClientUser) {
        this.transactionRepository = transactionRepository;
        this.webClientActiveCard = webClientActiveCard.baseUrl(activeCardUrl).build();
        this.webClientPasiveCard = webClientPasiveCard.baseUrl(pasiveCardUrl).build();
        this.webClientUser = webClientUser.baseUrl(userUrl).build();
    }

    private Mono<CurrentAccount> findCurrentAccountByDni(String dni, String account){
        return webClientPasiveCard.get().uri("/currentAccount/dni/" + dni + "/account/"+account).
                retrieve().bodyToMono(CurrentAccount.class);
    }
    private Mono<SavingAccount> findSavingAccountByDni(String dni, String account){
        return webClientPasiveCard.get().uri("/savingAccount/dni/" + dni + "/account/"+account).
                retrieve().bodyToMono(SavingAccount.class);
    }
    private Mono<FixedTermAccount> findFixedTermAccountByDni(String dni, String account){
        return webClientPasiveCard.get().uri("/fixedTermAccount/dni/" + dni + "/account/"+account).
                retrieve().bodyToMono(FixedTermAccount.class);
    }

    private Mono<CurrentAccount> updateCurrentAccount(CurrentAccount currentAccount){
        return webClientPasiveCard.put().uri("/currentAccount/" + currentAccount.getId()).
                body(Mono.just(currentAccount), CurrentAccount.class)
                .retrieve()
                .bodyToMono(CurrentAccount.class);
    }
    private Mono<SavingAccount> updateSavingAccount(SavingAccount savingAccount){
        return webClientPasiveCard.put().uri("/savingAccount/" + savingAccount.getId()).
                body(Mono.just(savingAccount), SavingAccount.class)
                .retrieve()
                .bodyToMono(SavingAccount.class);
    }
    private Mono<FixedTermAccount> updateFixedTermAccount(FixedTermAccount fixedTermAccount){
        return webClientPasiveCard.put().uri("/fixedTermAccount/" + fixedTermAccount.getId()).
                body(Mono.just(fixedTermAccount), FixedTermAccount.class)
                .retrieve()
                .bodyToMono(FixedTermAccount.class);
    }

    @Override
    public Flux<Transaction> findAll() {
        return transactionRepository.findAll();
    }

    @Override
    public Mono<Transaction> saveTransactionOfCurrentAccount(Transaction transaction) {
        transaction.setCommission(BigDecimal.valueOf(0));
        Mono<CurrentAccount> account= findCurrentAccountByDni(transaction.getDni(), transaction.getAccountNumber()).flatMap(x->{
            if(x.getCvc().equals(transaction.getCvc())){
                x.setBalance(x.getBalance().add(transaction.getAmount().negate()));
                return updateCurrentAccount(x);
            }
            return Mono.empty();
        });
        Mono<CurrentAccount> targetAccount= findCurrentAccountByDni(transaction.getTargetDni(), transaction.getTargetAccount()).flatMap(x->{
            x.setBalance(x.getBalance().add(transaction.getAmount()));
            return updateCurrentAccount(x);
        });
        return account.hasElement().flatMap(flag-> flag?targetAccount.then(transactionRepository.save(transaction)):Mono.empty());
    }

    @Override
    public Mono<Transaction> saveTransactionOfSavingAccount(Transaction transaction) {
        Mono<SavingAccount> account= findSavingAccountByDni(transaction.getDni(), transaction.getAccountNumber()).flatMap(x->{
            if(x.getCvc().equals(transaction.getCvc())) {
                if (x.getNumberTransactions().equals(0)) transaction.setCommission(BigDecimal.valueOf(4));
                else x.setNumberTransactions(x.getNumberTransactions() - 1);
                x.setBalance(x.getBalance().add(transaction.getAmount().negate()));
                return updateSavingAccount(x);
            }
            return Mono.empty();
        });
        Mono<SavingAccount> targetAccount= findSavingAccountByDni(transaction.getTargetDni(), transaction.getTargetAccount()).flatMap(x->{
            x.setBalance(x.getBalance().add(transaction.getAmount()));
            return updateSavingAccount(x);
        });
        return account.hasElement().flatMap(flag-> flag?targetAccount.then(transactionRepository.save(transaction)):Mono.empty());
    }

    @Override
    public Mono<Transaction> saveTransactionOfFixedTermAccount(Transaction transaction) {
        SimpleDateFormat dtf = new SimpleDateFormat("dd-MM-yyyy");
        Calendar calendar = Calendar.getInstance();
        Date dateObj = calendar.getTime();
        String formattedDate = dtf.format(dateObj);
        transaction.setCommission(BigDecimal.valueOf(0));
        System.out.println(formattedDate);
        Mono<FixedTermAccount> account= findFixedTermAccountByDni(transaction.getDni(), transaction.getAccountNumber()).flatMap(x->{
            if(x.getCvc().equals(transaction.getCvc()) && !x.getNumberTransactions().equals(0) && formattedDate.equals("24/04/2022")) {
                x.setNumberTransactions(x.getNumberTransactions() - 1);
                x.setBalance(x.getBalance().add(transaction.getAmount().negate()));
                return updateFixedTermAccount(x);
            }
            return Mono.empty();
        });
        Mono<FixedTermAccount> targetAccount= findFixedTermAccountByDni(transaction.getTargetDni(), transaction.getTargetAccount()).flatMap(x->{
            if(x.getNumberTransactions().equals(0)) transaction.setCommission(BigDecimal.valueOf(4));
            x.setBalance(x.getBalance().add(transaction.getAmount().negate()));
            return updateFixedTermAccount(x);
        });
        return account.hasElement().flatMap(flag->flag?targetAccount.then(transactionRepository.save(transaction)):Mono.empty());
    }

    @Override
    public Mono<Transaction> findById(String id) {
        return transactionRepository.findById(id);
    }

    @Override
    public Mono<Transaction> update(Transaction transaction, String id) {
        return transactionRepository.findById(id).flatMap(x->{
            x.setAmount(transaction.getAmount());
            x.setCurrency(transaction.getCurrency());
            x.setAccountNumber(transaction.getAccountNumber());
            x.setCvc(transaction.getCvc());
            return transactionRepository.save(x);
        });
    }

    @Override
    public Mono<Void> delete(String id) {
        return transactionRepository.deleteById(id);
    }
}
