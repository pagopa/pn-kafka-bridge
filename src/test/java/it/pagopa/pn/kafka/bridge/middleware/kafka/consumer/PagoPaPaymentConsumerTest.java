package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.service.PagoPaPaymentService;
import it.pagopa.pn.kafka.bridge.util.KafkaPaymentEventAgeCache;
import it.pagopa.pn.kafka.bridge.util.ValidatorUtil;
import jakarta.validation.Validation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoPaPaymentConsumerTest {

    @Mock
    private PagoPaPaymentService service;
    @Mock
    private ValidatorUtil validatorUtil;
    @Mock
    private ConsumerRecord<String, String> record;
    @Mock
    private KafkaPaymentEventAgeCache kafkaPaymentEventAgeCache;
    @Captor
    private ArgumentCaptor<List<PagoPaPaymentMessage>> payments;
    @Captor
    private ArgumentCaptor<Instant> ingestionTimestamp;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void usesKafkaPublicationTimestampAndListenerIngestionTimestamp() {
        Instant published = Instant.parse("2026-10-20T08:01:00Z");
        when(record.timestamp()).thenReturn(published.toEpochMilli());
        when(record.partition()).thenReturn(3);
        when(record.value()).thenReturn("""
                {"id":"payment-id","timestamp":"2026-10-20T08:00:00Z",
                 "pagopaPublishedTimestamp":"2000-01-01T00:00:00Z",
                 "modelType":"1","iuv":"iuv","iur":"iur","noticeNumber":"notice",
                 "idPA":"12345678901","companyName":"Comune",
                 "paymentDateTime":"2026-10-22T10:42:00Z","dueDate":"2026-12-31",
                 "totalNotice":"2","touchpoint":"APP","paymentChannel":"ATM",
                 "amount":"100","fee":"50"}
                """);
        Instant before = Instant.now();
        when(validatorUtil.isValidPaymentMessage(any(PagoPaPaymentMessage.class))).thenReturn(true);

        new PagoPaPaymentConsumer(service, validatorUtil, objectMapper, kafkaPaymentEventAgeCache).listen(List.of(record));

        Instant after = Instant.now();
        verify(service).sendMessage(payments.capture(), ingestionTimestamp.capture());
        PagoPaPaymentMessage payment = payments.getValue().getFirst();
        assertEquals(published, payment.getPagopaPublishedTimestamp());
        assertEquals(Instant.parse("2026-10-20T08:00:00Z"), payment.getTimestamp());
        assertEquals(3, payment.getKafkaPartition());
        assertEquals("notice", payment.getNoticeNumber());
        assertEquals("12345678901", payment.getIdPA());
        assertEquals("2", payment.getTotalNotice());
        assertEquals(100, payment.getAmount());
        assertEquals(50, payment.getFee());
        assertFalse(ingestionTimestamp.getValue().isBefore(before));
        assertFalse(ingestionTimestamp.getValue().isAfter(after));
    }

    @Test
    void rejectsRecordsWithoutKafkaTimestamp() {
        when(record.value()).thenReturn("{}");
        when(record.timestamp()).thenReturn(-1L);

        assertThrows(IllegalArgumentException.class,
                () -> new PagoPaPaymentConsumer(service, validatorUtil, objectMapper, kafkaPaymentEventAgeCache).listen(List.of(record)));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"id\":\"123456789012345678901\"}",
            "{\"id\":\"invalid\"}",
            "{invalid-json",
            "null"
    })
    void routesInvalidRecordToMalformedMessagesWithoutDiscardingValidRecords(String invalidBody) {
        assertMalformedRouting(invalidBody);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2147483648", "-2147483649", "-1", "+1", "", "1.5", " 2", "2 "})
    void routesInvalidTotalNoticeWithoutDiscardingValidRecords(String totalNotice) {
        String invalidBody = PaymentFlowFixtures.validPayment(1)
                .replace("\"totalNotice\": \"2\"", "\"totalNotice\": \"" + totalNotice + "\"");

        assertMalformedRouting(invalidBody);
    }

    private void assertMalformedRouting(String invalidBody) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var consumer = new PagoPaPaymentConsumer(service,
                    new ValidatorUtil(factory.getValidator()), objectMapper, kafkaPaymentEventAgeCache);
            consumer.listen(List.of(
                    PaymentFlowFixtures.record(0, PaymentFlowFixtures.validPayment(0)),
                    PaymentFlowFixtures.record(1, invalidBody),
                    PaymentFlowFixtures.record(2, PaymentFlowFixtures.validPayment(2))));

            verify(service).sendMalformedMessage(List.of(invalidBody));
            verify(service).sendMessage(payments.capture(), ingestionTimestamp.capture());
            assertEquals(List.of("payment-0", "payment-2"),
                    payments.getValue().stream().map(PagoPaPaymentMessage::getId).toList());
        }
    }
}
