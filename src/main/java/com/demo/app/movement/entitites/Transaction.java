package com.demo.app.movement.entitites;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

@Document(collection = "transaction")
@Data
public class Transaction extends Audit{
    @Id
    private String id;

    @NotEmpty
    private String movementId;

    @NotEmpty
    private String description;

    private Boolean type;

    @Field(name = "account_number")
    @Size(min = 16,max = 16)
    private String accountNumber;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @NotEmpty
    @Size(min = 8,max = 11)
    private String identifier;

    @Enumerated(EnumType.STRING)
    private TypeCurrency currency;
}
