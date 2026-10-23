package it.pagopa.pn.kafka.bridge.service;

import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;

import java.time.Instant;
import java.util.List;

public interface PagoPaPaymentService {
    void sendMessage(List<String> inputMessages);
    void sendMessage(List<PagoPaPaymentMessage> inputMessages, Instant timestamp);
}
