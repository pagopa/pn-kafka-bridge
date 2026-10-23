package it.pagopa.pn.kafka.bridge.model;

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
}
