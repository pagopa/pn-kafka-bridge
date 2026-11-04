package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

@Data
public class PagoPaPaymentMessage {
    @NotNull
    @Length(max = 20)
    private String id;
    private Instant timestamp;
    @Valid
    private DebtorPosition debtorPosition;
    @Valid
    private Creditor creditor;
    @Valid
    private PaymentInfo paymentInfo;
    //Campo valorizzato tramite header del messaggio di kafka
    @JsonIgnore
    private Instant pagopaPublishedTimestamp;
}
