package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.service.PagoPaPaymentService;
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
    @Captor
    private ArgumentCaptor<List<PagoPaPaymentMessage>> payments;
    @Captor
    private ArgumentCaptor<Instant> ingestionTimestamp;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void usesKafkaPublicationTimestampAndListenerIngestionTimestamp() {
        Instant published = Instant.parse("2026-10-20T08:01:00Z");
        when(record.timestamp()).thenReturn(published.toEpochMilli());
        when(record.value()).thenReturn("""
                {"id":"payment-id","timestamp":"2026-10-20T08:00:00Z",
                 "pagopaPublishedTimestamp":"2000-01-01T00:00:00Z",
                 "debtorPosition":{"noticeNumber":"notice"},
                 "creditor":{"idPA":"12345678901"},
                 "paymentInfo":{"totalNotice":"2","fee":50}}
                """);
        Instant before = Instant.now();
        when(validatorUtil.isValidPaymentMessage(any(PagoPaPaymentMessage.class))).thenReturn(true);

        new PagoPaPaymentConsumer(service, validatorUtil, objectMapper).listen(List.of(record));

        Instant after = Instant.now();
        verify(service).sendMessage(payments.capture(), ingestionTimestamp.capture());
        PagoPaPaymentMessage payment = payments.getValue().get(0);
        assertEquals(published, payment.getPagopaPublishedTimestamp());
        assertEquals(Instant.parse("2026-10-20T08:00:00Z"), payment.getTimestamp());
        assertEquals("notice", payment.getDebtorPosition().getNoticeNumber());
        assertEquals("12345678901", payment.getCreditor().getIdPA());
        assertEquals("2", payment.getPaymentInfo().getTotalNotice());
        assertEquals(Integer.valueOf(50), payment.getPaymentInfo().getFee());
        assertFalse(ingestionTimestamp.getValue().isBefore(before));
        assertFalse(ingestionTimestamp.getValue().isAfter(after));
    }

    @Test
    void rejectsRecordsWithoutKafkaTimestamp() {
        when(record.value()).thenReturn("{}");
        when(record.timestamp()).thenReturn(-1L);

        assertThrows(IllegalArgumentException.class,
                () -> new PagoPaPaymentConsumer(service, validatorUtil, objectMapper).listen(List.of(record)));

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"id\":\"123456789012345678901\"}",
            "{\"id\":\"invalid\",\"debtorPosition\":{}}",
            "{invalid-json",
            "null"
    })
    void routesInvalidRecordToMalformedMessagesWithoutDiscardingValidRecords(String invalidBody) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var consumer = new PagoPaPaymentConsumer(service,
                    new ValidatorUtil(factory.getValidator()), objectMapper);
            consumer.listen(List.of(
                    PaymentFlowFixtures.record(0, "{\"id\":\"valid-before\"}"),
                    PaymentFlowFixtures.record(1, invalidBody),
                    PaymentFlowFixtures.record(2, "{\"id\":\"valid-after\"}")));

            verify(service).sendMessage(List.of(invalidBody));
            verify(service).sendMessage(payments.capture(), ingestionTimestamp.capture());
            assertEquals(List.of("valid-before", "valid-after"),
                    payments.getValue().stream().map(PagoPaPaymentMessage::getId).toList());
        }
    }
}
