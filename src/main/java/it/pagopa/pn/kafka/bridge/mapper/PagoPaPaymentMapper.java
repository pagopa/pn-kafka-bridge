package it.pagopa.pn.kafka.bridge.mapper;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.kafka.bridge.model.Creditor;
import it.pagopa.pn.kafka.bridge.model.DebtorPosition;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.model.PaymentInfo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class PagoPaPaymentMapper {

    public List<PnErrorEvent> buildMalformedEvents(List<String> inputMessages) {
        return inputMessages.stream().map(message -> PnErrorEvent.builder()
                .header(StandardEventHeader.builder()
                        .createdAt(Instant.now())
                        .eventType(EventType.ERROR_EVENT.name())
                        .publisher(EventPublisher.KAFKA_BRIDGE.name())
                        .build())
                .payload(PnErrorEvent.Payload.builder()
                        .originalMessage(message)
                        .timestamp(Instant.now())
                        .description("Error: message malformed.")
                        .build())
                .build())
                .toList();
    }

    public List<PnNoticePaymentEvent> buildEvents(List<PagoPaPaymentMessage> inputMessages, Instant timestamp) {
        return inputMessages.stream()
                .map(message -> toPnNoticePaymentEvent(message, timestamp))
                .toList();
    }

    public PnNoticePaymentEvent toPnNoticePaymentEvent(PagoPaPaymentMessage message, Instant ingestionTimestamp) {
        DebtorPosition debtorPosition = message.getDebtorPosition();
        Creditor creditor = message.getCreditor();
        PaymentInfo paymentInfo = message.getPaymentInfo();

        return PnNoticePaymentEvent.builder()
                .header(StandardEventHeader.builder()
                        .createdAt(Instant.now())
                        .eventType(EventType.NOTICE_PAYMENT.name())
                        .publisher(EventPublisher.KAFKA_BRIDGE.name())
                        .build())
                .payload(PnNoticePaymentEvent.Payload.builder()
                        .modelType(debtorPosition.getModelType())
                        .iuv(debtorPosition.getIuv())
                        .iur(debtorPosition.getIur())
                        .noticeCode(debtorPosition.getNoticeNumber())
                        .creditorTaxId(creditor.getIdPA())
                        .creditorCompanyName(creditor.getCompanyName())
                        .paymentId(message.getId())
                        .paymentDateTime(message.getTimestamp())
                        .dueDate(paymentInfo.getDueDate())
                        .totalNotice(paymentInfo.getTotalNotice() == null
                                ? null : Integer.valueOf(paymentInfo.getTotalNotice()))
                        .paymentSourceChannel(paymentInfo.getPaymentChannel())
                        .amount(paymentInfo.getAmount())
                        .fee(paymentInfo.getFee() == null ? null : paymentInfo.getFee().toString())
                        .pagopaPublishedTimestamp(message.getPagopaPublishedTimestamp())
                        .sendIngestionTimestamp(ingestionTimestamp)
                        .build())
                .build();
    }
}
