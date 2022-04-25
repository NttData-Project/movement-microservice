package com.demo.app.movement.models;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedTermAccount {
    private String id;
    private BigDecimal balance;
    private TypeCurrency currency;
    private String accountNumber;
    private Integer cvc;
    private String identifier;
    private Integer numberTransactions;
}
