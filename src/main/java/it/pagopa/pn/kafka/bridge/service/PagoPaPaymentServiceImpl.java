package it.pagopa.pn.kafka.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnErrorEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.kafka.bridge.mapper.PagoPaPaymentMapper;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Service
@CustomLog
@RequiredArgsConstructor
public class PagoPaPaymentServiceImpl implements PagoPaPaymentService {

    private static final int BATCH_SIZE = 10;
    private static final int MAX_BATCH_BYTES = 1_048_576;
    private static final int LOG_PREVIEW_LENGTH = 256;

    private final MomProducer<PnErrorEvent> errorEvent;
    private final MomProducer<PnNoticePaymentEvent> noticePaymentEvent;
    private final ObjectMapper objectMapper;
    private final PagoPaPaymentMapper mapper;

    @Override
    public void sendMessage(List<String> inputMessages) {
        List<PnErrorEvent> events = mapper.buildMalformedEvents(inputMessages);
        buildMalformedBatches(events).parallelStream()
                .forEach(batch -> {
                    log.debug("Sending messages to SQS: {}", batch);
                    errorEvent.push(batch);
                    log.info("Message sent to SQS: {}", batch);
                });
    }

    List<List<PnErrorEvent>> buildMalformedBatches(List<PnErrorEvent> events) {
        List<List<PnErrorEvent>> batches = new ArrayList<>();
        List<PnErrorEvent> batch = new ArrayList<>();
        int batchBytes = 0;
        for (PnErrorEvent event : events) {
            int eventBytes = malformedEventSize(event);
            if (eventBytes > MAX_BATCH_BYTES) {
                logOversizedEvent(event, eventBytes);
                continue;
            }
            if (batch.size() == BATCH_SIZE || batchBytes + eventBytes > MAX_BATCH_BYTES) {
                batches.add(batch);
                batch = new ArrayList<>();
                batchBytes = 0;
            }
            batch.add(event);
            batchBytes += eventBytes;
        }
        if (!batch.isEmpty()) {
            batches.add(batch);
        }
        return batches;
    }

    private void logOversizedEvent(PnErrorEvent event, int eventBytes) {
        String message = event.getPayload().getOriginalMessage();
        int previewEnd = message.offsetByCodePoints(0,
                Math.min(LOG_PREVIEW_LENGTH, message.codePointCount(0, message.length())));
        log.fatal("Skipping oversized malformed event: {} bytes exceeds {} bytes; first 256 characters: {}",
                eventBytes, MAX_BATCH_BYTES, message.substring(0, previewEnd));
    }

    private int malformedEventSize(PnErrorEvent event) {
        try {
            int payloadBytes = objectMapper.writerFor(PnErrorEvent.Payload.class)
                    .writeValueAsString(event.getPayload()).getBytes(StandardCharsets.UTF_8).length;
            StandardEventHeader header = event.getHeader();
            // AbstractSqsMomProducer sends the payload as JSON and these five String attributes.
            return payloadBytes
                    + attributeSize("eventId", header.getEventId())
                    + attributeSize("eventType", header.getEventType())
                    + attributeSize("createdAt", header.getCreatedAt().toString())
                    + attributeSize("publisher", header.getPublisher())
                    + attributeSize("iun", header.getIun());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize malformed event for SQS size validation", e);
        }
    }

    private int attributeSize(String name, String value) {
        return name.length() + "String".length()
                + (value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length);
    }

    @Override
    public void sendMessage(List<PagoPaPaymentMessage> inputMessages, Instant timestamp) {
        List<PnNoticePaymentEvent> events = mapper.buildEvents(inputMessages, timestamp);
        IntStream.range(0, Math.ceilDiv(events.size(), BATCH_SIZE))
                .parallel()
                .map(index -> index * BATCH_SIZE)
                .mapToObj(start -> events.subList(start, Math.min(start + BATCH_SIZE, events.size())))
                .forEach(batch -> {
                    log.debug("Sending messages to SQS: {}", batch);
                    noticePaymentEvent.push(batch);
                    log.info("Message sent to SQS: {}", batch);
                });
    }

}
