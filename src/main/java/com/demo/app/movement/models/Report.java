package com.demo.app.movement.models;

import lombok.Data;

import java.util.List;

@Data
public class Report {
    private String id;
    private String dni;
    private String name;
    private String lastName;
    private String commission;
    private List<CurrentAccount> currentAccounts;
    private List<SavingAccount> savingAccounts;
    private List<FixedTermAccount> fixedTermAccounts;
}
