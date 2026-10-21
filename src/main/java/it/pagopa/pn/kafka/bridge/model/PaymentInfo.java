package it.pagopa.pn.kafka.bridge.model;

import lombok.Data;

import java.time.Instant;

@Data
public class PaymentInfo {
    private Instant dueDate;
    private String totalNotice;
    private String paymentChannel;
    private Integer amount;
    private Integer fee;
}
