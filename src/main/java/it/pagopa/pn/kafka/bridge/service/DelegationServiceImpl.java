package it.pagopa.pn.kafka.bridge.service;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnPtDelegationEvent;
import it.pagopa.pn.api.dto.events.PnPtDelegationPayload;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.kafka.bridge.mapper.DelegationMapper;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@CustomLog
@RequiredArgsConstructor
public class DelegationServiceImpl implements DelegationService {

    private final MomProducer<PnPtDelegationEvent> delegationProducer;

    private final DelegationMapper mapper;

    @Override
    public void sendMessage(DelegationSelfCareMessage inputMessage) {
        PnPtDelegationPayload payload = mapper.toPnPtDelegationPayload(inputMessage);
        PnPtDelegationEvent event = buildEvent(payload);
        log.debug("[{}] Sending message to SQS", payload.getDelegationId());
        delegationProducer.push(event);
        log.info("[{}] Message sent to SQS: {}", payload.getDelegationId(), event);
    }

    private PnPtDelegationEvent buildEvent(PnPtDelegationPayload payload) {
        String eventId = payload.getDelegationId() + "_" + payload.getSourceEventId();
        return PnPtDelegationEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(payload.getDelegationId())
                        .eventId(eventId)
                        .createdAt(Instant.now())
                        .eventType(EventType.PT_DELEGATION_CHANGED.name())
                        .publisher(EventPublisher.KAFKA_BRIDGE.name())
                        .build()
                )
                .payload(payload)
                .build();
    }
}
