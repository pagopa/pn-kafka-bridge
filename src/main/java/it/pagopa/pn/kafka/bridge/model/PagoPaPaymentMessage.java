package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class PagoPaPaymentMessage {
    @NotNull
    private String id;
    private Instant timestamp;
    @NotNull
    @Valid
    private DebtorPosition debtorPosition;
    @NotNull
    @Valid
    private Creditor creditor;
    @NotNull
    @Valid
    private PaymentInfo paymentInfo;
    //Campo valorizzato tramite header del messaggio di kafka
    @JsonIgnore
    private Instant pagopaPublishedTimestamp;
}
