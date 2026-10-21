package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.service.PagoPaPaymentService;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.util.Tuple;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@CustomLog
@AllArgsConstructor
public class PagoPaPaymentConsumer {

    private final PagoPaPaymentService pagoPaPaymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            id = "${pn.kafka-bridge.payment-events.group-id}",
            topics = "${pn.kafka-bridge.payment-events.topic-name}",
            containerFactory = "paymentEventsKafkaListenerContainerFactory")
    public void listen(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Payload List<ConsumerRecord<String, String>> records) {
        log.info("Received message from topic: {}, with value: {}", topic, records.stream().map(ConsumerRecord::key).toList());

        Instant timestamp = Instant.now();
        Tuple<List<String>, List<PagoPaPaymentMessage>> arrays = createArraysForSqs(records);
        pagoPaPaymentService.sendMessage(arrays._1());
        pagoPaPaymentService.sendMessage(arrays._2(), timestamp);
    }

    private Tuple<List<String>, List<PagoPaPaymentMessage>> createArraysForSqs(List<ConsumerRecord<String, String>> records) {
        ArrayList<String> malformedMessages = new ArrayList<>();
        ArrayList<PagoPaPaymentMessage> validMessages = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            PagoPaPaymentMessage message = createPagoPaPaymentMessage(record);
            if (message == null) {
                malformedMessages.add(record.value());
            } else {
                validMessages.add(message);
            }
        }

        return new Tuple<>(malformedMessages, validMessages);
    }

    private PagoPaPaymentMessage createPagoPaPaymentMessage(ConsumerRecord<String, String> message) {
        try {
            PagoPaPaymentMessage payment = objectMapper.readValue(message.value(), PagoPaPaymentMessage.class);
            if (message.timestamp() < 0) {
                throw new IllegalArgumentException("Missing Kafka timestamp for payment record at offset " + message.offset());
            }
            payment.setPagopaPublishedTimestamp(Instant.ofEpochMilli(message.timestamp()));
            return payment;
        } catch (JacksonException e) {
            log.error("Error deserializing message: {}", message.value(), e);
            return null;
        }
    }
}
