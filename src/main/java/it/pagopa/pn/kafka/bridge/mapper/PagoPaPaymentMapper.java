package it.pagopa.pn.kafka.bridge.mapper;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PagoPaPaymentMapper {

    public List<PnErrorEvent> buildMalformedEvents(List<String> inputMessages) {
        return inputMessages.stream().map(message -> PnErrorEvent.builder()
                .header(GenericEventHeader.builder()
                        .eventId("MALFORMED_EVENT_" + UUID.randomUUID())
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
        return PnNoticePaymentEvent.builder()
                .header(GenericEventHeader.builder()
                        .eventId("NOTICE_PAYMENT_EVENT_" + UUID.randomUUID())
                        .createdAt(Instant.now())
                        .eventType(EventType.NOTICE_PAYMENT.name())
                        .publisher(EventPublisher.KAFKA_BRIDGE.name())
                        .build())
                .payload(PnNoticePaymentEvent.Payload.builder()
                        .modelType(message.getModelType())
                        .iuv(message.getIuv())
                        .iur(message.getIur())
                        .noticeCode(message.getNoticeNumber())
                        .creditorTaxId(message.getIdPA())
                        .creditorCompanyName(message.getCompanyName())
                        .paymentId(message.getId())
                        .paymentDateTime(message.getPaymentDateTime())
                        .dueDate(message.getDueDate())
                        .totalNotice(message.getTotalNotice() == null
                                ? null : Integer.valueOf(message.getTotalNotice()))
                        .paymentSourceChannel(message.getPaymentChannel())
                        .paymentMethod(message.getTouchpoint())
                        .amount(message.getAmount())
                        .fee(message.getFee() == null ? null : message.getFee().toString())
                        .pagopaPublishedTimestamp(message.getPagopaPublishedTimestamp())
                        .sendIngestionTimestamp(ingestionTimestamp)
                        .pagopaPersistedTimestamp(message.getTimestamp() == null
                                ? null : Instant.ofEpochMilli(message.getTimestamp()))
                        .kafkaPartition(message.getKafkaPartition() != null ? message.getKafkaPartition().toString() : null)
                        .build())
                .build();
    }
}
