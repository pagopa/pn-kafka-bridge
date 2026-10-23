package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class Creditor {
    @NotNull
    @Length(max = 1000)
    private String idPA;
    @Length(max = 1000)
    private String companyName;
}
