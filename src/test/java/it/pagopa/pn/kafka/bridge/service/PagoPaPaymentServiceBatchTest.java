package it.pagopa.pn.kafka.bridge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnErrorEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.kafka.bridge.model.Creditor;
import it.pagopa.pn.kafka.bridge.model.DebtorPosition;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import it.pagopa.pn.kafka.bridge.model.PaymentInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.time.Instant;
import java.util.stream.IntStream;

import static java.util.Comparator.comparingInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PagoPaPaymentServiceBatchTest {

    @Mock
    private MomProducer<PnErrorEvent> errorEvent;
    @Mock
    private MomProducer<PnNoticePaymentEvent> noticePaymentEvent;
    private PagoPaPaymentServiceImpl service;
    @Captor
    private ArgumentCaptor<List<PnErrorEvent>> batches;
    @Captor
    private ArgumentCaptor<List<PnNoticePaymentEvent>> paymentBatches;

    @BeforeEach
    void setUp() {
        service = new PagoPaPaymentServiceImpl(errorEvent, noticePaymentEvent,
                new ObjectMapper().findAndRegisterModules(), new PagoPaPaymentMapper());
    }

    @Test
    void mapsPaymentFieldsAndDistinctTimestamps() {
        PagoPaPaymentMessage message = paymentMessage();
        message.setId("payment-id");
        message.setTimestamp(Instant.parse("2026-10-20T08:00:00Z"));
        message.setPagopaPublishedTimestamp(Instant.parse("2026-10-20T08:01:00Z"));
        message.getDebtorPosition().setModelType("1");
        message.getDebtorPosition().setIuv("iuv");
        message.getDebtorPosition().setIur("iur");
        message.getDebtorPosition().setNoticeNumber("notice");
        message.getCreditor().setIdPA("12345678901");
        message.getCreditor().setCompanyName("creditor");
        message.getPaymentInfo().setDueDate(Instant.parse("2026-10-21T00:00:00Z"));
        message.getPaymentInfo().setTotalNotice("2");
        message.getPaymentInfo().setPaymentChannel("channel");
        message.getPaymentInfo().setAmount(1000);
        message.getPaymentInfo().setFee(50);
        Instant ingestionTimestamp = Instant.parse("2026-10-20T08:02:00Z");

        service.sendMessage(List.of(message), ingestionTimestamp);

        verify(noticePaymentEvent).push(paymentBatches.capture());
        PnNoticePaymentEvent event = paymentBatches.getValue().get(0);
        assertEquals(EventType.NOTICE_PAYMENT.name(), event.getHeader().getEventType());
        assertEquals(EventPublisher.KAFKA_BRIDGE.name(), event.getHeader().getPublisher());
        assertEquals(PnNoticePaymentEvent.Payload.builder()
                .modelType("1").iuv("iuv").iur("iur").noticeCode("notice")
                .creditorTaxId("12345678901").creditorCompanyName("creditor")
                .paymentId("payment-id").paymentDateTime(message.getTimestamp())
                .dueDate(message.getPaymentInfo().getDueDate()).totalNotice(2)
                .paymentSourceChannel("channel").amount(1000).fee("50")
                .pagopaPublishedTimestamp(message.getPagopaPublishedTimestamp())
                .sendIngestionTimestamp(ingestionTimestamp).build(), event.getPayload());
        verifyNoInteractions(errorEvent);
    }

    @Test
    void preservesNullOptionalNumericFields() {
        service.sendMessage(List.of(paymentMessage()), Instant.EPOCH);

        verify(noticePaymentEvent).push(paymentBatches.capture());
        assertEquals(PnNoticePaymentEvent.Payload.builder()
                .sendIngestionTimestamp(Instant.EPOCH).build(), paymentBatches.getValue().get(0).getPayload());
    }

    @Test
    void rejectsInvalidTotalNoticeBeforeSendingAnyEvents() {
        PagoPaPaymentMessage invalid = paymentMessage();
        invalid.getPaymentInfo().setTotalNotice("not-a-number");

        assertThrows(NumberFormatException.class,
                () -> service.sendMessage(List.of(paymentMessage(), invalid), Instant.EPOCH));

        verifyNoInteractions(noticePaymentEvent, errorEvent);
    }

    private PagoPaPaymentMessage paymentMessage() {
        PagoPaPaymentMessage message = new PagoPaPaymentMessage();
        message.setDebtorPosition(new DebtorPosition());
        message.setCreditor(new Creditor());
        message.setPaymentInfo(new PaymentInfo());
        return message;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10, 11, 20, 23})
    void sendsBatchesOfAtMostTenPreservingOrderWithinEachBatch(int count) {
        List<String> messages = IntStream.range(0, count)
                .mapToObj(index -> "message-" + index)
                .toList();

        service.sendMessage(messages);

        verify(errorEvent, times((count + 9) / 10)).push(batches.capture());
        List<List<PnErrorEvent>> sentBatches = batches.getAllValues().stream()
                .sorted(comparingInt(batch -> messages.indexOf(batch.get(0).getPayload().getOriginalMessage())))
                .toList();
        assertEquals(IntStream.iterate(0, start -> start < count, start -> start + 10)
                        .map(start -> Math.min(10, count - start)).boxed().toList(),
                sentBatches.stream().map(List::size).toList());
        assertEquals(messages, sentBatches.stream().flatMap(List::stream)
                .map(event -> event.getPayload().getOriginalMessage()).toList());
        verifyNoInteractions(noticePaymentEvent);
    }
}
