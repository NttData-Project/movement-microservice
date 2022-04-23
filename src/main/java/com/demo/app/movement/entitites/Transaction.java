package com.demo.app.movement.entitites;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@JsonPropertyOrder({"id","dni","accountNumber","targetDni","targetAccount","amount","currency","cvc","commission","createAt","updateAt"})
@Document(collection = "transaction")
@Data
public class Transaction extends Audit{
    @Id
    private String id;

    @Field(name = "account_number")
    @Size(min = 16,max = 16)
    private String accountNumber;

    @Field(name = "target_account")
    @Size(min = 16,max = 16)
    private String targetAccount;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @NotEmpty
    @Size(min = 8,max = 8)
    private String dni;

    @NotEmpty
    @Size(min = 8,max = 8)
    private String targetDni;

    @Enumerated(EnumType.STRING)
    private TypeCurrency currency;

    @Range(min = 100,max = 999)
    private Integer cvc;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal commission;
}
