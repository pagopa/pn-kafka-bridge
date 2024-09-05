package it.pagopa.pn.kafka.bridge.service;

import it.pagopa.pn.api.dto.events.*;
import it.pagopa.pn.kafka.bridge.mapper.OnboardingMapper;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final MomProducer<PnOnboardingInstitutionEvent> notificationPaidProducer;

    private final OnboardingMapper mapper;

    public void sendMessage(OnboardingSelfCareMessage inputMessage) {
        PnOnboardInstitutionPayload payload = mapper.toPnOnboardInstitutionPayload(inputMessage);
        PnOnboardingInstitutionEvent event = buildEvent(payload);
        log.debug("[{}] Sending message to SQS", payload.getId());
        notificationPaidProducer.push(event);
        log.info("[{}] Message sent to SQS: {}", payload.getId(), event);
    }

    private PnOnboardingInstitutionEvent buildEvent(PnOnboardInstitutionPayload payload) {
        String eventId = payload.getTaxCode() + "_onboarding_institution_" + payload.getId();
        return PnOnboardingInstitutionEvent.builder()
                .header(StandardEventHeader.builder()
                        .iun(payload.getTaxCode())
                        .eventId(eventId)
                        .createdAt(Instant.now())
                        .eventType(EventType.SEND_ONBOARDING_REQUEST.name())
                        .publisher(EventPublisher.KAFKA_BRIDGE.name())
                        .build()
                )
                .payload(payload)
                .build();
    }
}
