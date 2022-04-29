package com.demo.app.movement.entitites;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;

@JsonPropertyOrder({"id", "description", "dni", "accountNumber", "targetDni", "targetAccount", "amount", "currency", "cvc", "commission", "createAt", "updateAt"})
@Document(collection = "movement")
@Data
public class Movement extends Audit {
    @Id
    private String id;

    @NotEmpty
    private String description;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    @NotEmpty
    private String identifier;

    private Boolean type;

    @NotEmpty
    private String targetIdentifier;

    @Enumerated(EnumType.STRING)
    private TypeCurrency currency;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Range(min = 100, max = 999)
    private Integer cvc;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal commission;
}
