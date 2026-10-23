package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.service.PagoPaPaymentService;
import it.pagopa.pn.kafka.bridge.util.ValidatorUtil;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
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
    private final ValidatorUtil validatorUtil;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            id = "${pn.kafka-bridge.payment-events.group-id}",
            topics = "${pn.kafka-bridge.payment-events.topic-name}",
            containerFactory = "paymentEventsKafkaListenerContainerFactory")
    public void listen(List<ConsumerRecord<String, String>> records) {
        log.info("Received messages from topics: {}, with keys: {}",
                records.stream().map(ConsumerRecord::topic).distinct().toList(),
                records.stream().map(ConsumerRecord::key).toList());
        Instant timestamp = Instant.now();
        Tuple<List<String>, List<PagoPaPaymentMessage>> arrays = createArraysForSqs(records);

        pagoPaPaymentService.sendMessage(arrays._2(), timestamp);
        pagoPaPaymentService.sendMessage(arrays._1());
    }

    private Tuple<List<String>, List<PagoPaPaymentMessage>> createArraysForSqs(List<ConsumerRecord<String, String>> records) {
        ArrayList<String> malformedMessages = new ArrayList<>();
        ArrayList<PagoPaPaymentMessage> validMessages = new ArrayList<>();

        for (ConsumerRecord<String, String> record : records) {
            PagoPaPaymentMessage message = createPagoPaPaymentMessage(record);
            if (message == null || !validatorUtil.isValidPaymentMessage(message)) {
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
            if (payment == null) {
                log.error("Null payment payload from topic {} partition {} at offset {}",
                        message.topic(), message.partition(), message.offset());
                return null;
            }
            if (message.timestamp() < 0) {
                throw new IllegalArgumentException("Missing Kafka timestamp for payment record at offset " + message.offset());
            }
            payment.setPagopaPublishedTimestamp(Instant.ofEpochMilli(message.timestamp()));
            return payment;
        } catch (JacksonException e) {
            log.error("Error deserializing payment record from topic {} partition {} at offset {} ({})",
                    message.topic(), message.partition(), message.offset(), e.getClass().getSimpleName());
            return null;
        }
    }
}
