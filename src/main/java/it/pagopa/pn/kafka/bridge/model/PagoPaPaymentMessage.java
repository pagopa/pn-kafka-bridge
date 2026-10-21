package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.Instant;

@Data
public class PagoPaPaymentMessage {
    private String id;
    private Instant timestamp;
    private DebtorPosition debtorPosition;
    private Creditor creditor;
    private PaymentInfo paymentInfo;
    @JsonIgnore
    private Instant pagopaPublishedTimestamp;
}
