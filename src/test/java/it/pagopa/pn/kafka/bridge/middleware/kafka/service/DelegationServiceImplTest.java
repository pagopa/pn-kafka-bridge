package it.pagopa.pn.kafka.bridge.middleware.kafka.service;

import it.pagopa.pn.api.dto.events.EventPublisher;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.api.dto.events.PnPtDelegationEvent;
import it.pagopa.pn.kafka.bridge.mapper.DelegationMapper;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.DelegationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DelegationServiceImplTest {

    private static final String DELEGATION_ID = "6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22";
    private static final String SOURCE_EVENT_ID = "0f3a9a52-5a3b-4d0c-9f0e-0d8a5d6f1a11";

    @Mock
    private MomProducer<PnPtDelegationEvent> delegationProducer;

    @Spy
    private DelegationMapper mapper;

    @InjectMocks
    private DelegationServiceImpl service;

    @Test
    void sendMessageValidMessagePushesEventWithExpectedHeaderTest() {
        Instant before = Instant.now();

        service.sendMessage(message());

        ArgumentCaptor<PnPtDelegationEvent> captor = ArgumentCaptor.forClass(PnPtDelegationEvent.class);
        Mockito.verify(delegationProducer, Mockito.times(1)).push(captor.capture());
        PnPtDelegationEvent event = captor.getValue();
        assertThat(event.getHeader().getIun()).isEqualTo(DELEGATION_ID);
        assertThat(event.getHeader().getEventId()).isEqualTo(DELEGATION_ID + "_" + SOURCE_EVENT_ID);
        assertThat(event.getHeader().getEventType()).isEqualTo(EventType.PT_DELEGATION_CHANGED.name());
        assertThat(event.getHeader().getPublisher()).isEqualTo(EventPublisher.KAFKA_BRIDGE.name());
        assertThat(event.getHeader().getCreatedAt()).isNotNull().isAfterOrEqualTo(before);
    }

    @Test
    void sendMessageValidMessageEventIdCompatibleWithSqsBatchEntryIdTest() {
        service.sendMessage(message());

        ArgumentCaptor<PnPtDelegationEvent> captor = ArgumentCaptor.forClass(PnPtDelegationEvent.class);
        Mockito.verify(delegationProducer).push(captor.capture());
        String eventId = captor.getValue().getHeader().getEventId();
        assertThat(eventId).hasSizeLessThanOrEqualTo(80);
        assertThat(eventId).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void sendMessageValidMessagePushesMappedPayloadTest() {
        DelegationSelfCareMessage message = message();

        service.sendMessage(message);

        ArgumentCaptor<PnPtDelegationEvent> captor = ArgumentCaptor.forClass(PnPtDelegationEvent.class);
        Mockito.verify(delegationProducer).push(captor.capture());
        assertThat(captor.getValue().getPayload().getDelegationId()).isEqualTo(DELEGATION_ID);
        assertThat(captor.getValue().getPayload().getSourceEventId()).isEqualTo(SOURCE_EVENT_ID);
        assertThat(captor.getValue().getPayload().getDelegationType()).isEqualTo("PT");
        assertThat(captor.getValue().getPayload().getProductId()).isEqualTo("prod-pn-dev");
        assertThat(captor.getValue().getPayload().getPtTaxCode()).isEqualTo("01234567890");
        assertThat(captor.getValue().getPayload().getInstitutionTaxCode()).isEqualTo("00488410010");
    }

    private DelegationSelfCareMessage message() {
        DelegationSelfCareMessage message = new DelegationSelfCareMessage();
        message.setId(SOURCE_EVENT_ID);
        message.setDelegationId(DELEGATION_ID);
        message.setFrom("b1f3c2d4-0a1b-4c2d-8e3f-4a5b6c7d8e9f");
        message.setInstitutionFromName("Comune di Milano");
        message.setInstitutionToName("Partner Tecnologico Srl");
        message.setTo("c2a4d3e5-1b2c-4d3e-9f40-5b6c7d8e9f01");
        message.setToTaxCode("01234567890");
        message.setFromTaxCode("00488410010");
        message.setToType("PT");
        message.setFromType("PA");
        message.setProductId("prod-pn-dev");
        message.setType("PT");
        message.setStatus("ACTIVE");
        message.setCreatedAt(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        return message;
    }
}
