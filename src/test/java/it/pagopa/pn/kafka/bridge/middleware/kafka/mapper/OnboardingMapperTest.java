package it.pagopa.pn.kafka.bridge.middleware.kafka.mapper;

import it.pagopa.pn.api.dto.events.PnOnboardInstitutionPayload;
import it.pagopa.pn.kafka.bridge.mapper.OnboardingMapper;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingMapperTest {

    OnboardingMapper onboardingMapper = new OnboardingMapper();

    @Test
    void toPnOnboardInstitutionPayloadTest() {
        OnboardingSelfCareMessage inputMessage = createInputMessage();

        PnOnboardInstitutionPayload actual = onboardingMapper.toPnOnboardInstitutionPayload(inputMessage);

        assertThat(actual.getId()).isEqualTo(inputMessage.getInternalIstitutionID());
        assertThat(actual.getStatus()).isEqualTo(inputMessage.getState());
        assertThat(actual.getLastUpdate()).isEqualTo(inputMessage.getUpdatedAt());
        assertThat(actual.getTaxCode()).isEqualTo(inputMessage.getInstitution().getTaxCode());
        assertThat(actual.getDescription()).isEqualTo(inputMessage.getInstitution().getDescription());
        assertThat(actual.getAddress()).isEqualTo(inputMessage.getInstitution().getAddress());
        assertThat(actual.getDigitalAddress()).isEqualTo(inputMessage.getInstitution().getDigitalAddress());
        assertThat(actual.getExternalId()).isEqualTo(inputMessage.getOnboardingTokenId());
        assertThat(actual.getCreated()).isEqualTo(inputMessage.getCreatedAt());
        assertThat(actual.getZipCode()).isEqualTo(inputMessage.getZipCode());
    }

    private OnboardingSelfCareMessage createInputMessage() {
        OnboardingSelfCareMessage.Institution institution = new OnboardingSelfCareMessage.Institution();
        institution.setAddress("Piazza Umberto I, 1");
        institution.setOrigin("IPA");
        institution.setInstitutionType("PA");
        institution.setDescription("Comune di Tovo San Giacomo");
        institution.setTaxCode("00338460090");
        institution.setDigitalAddress("protocol@comunetovosangiacomo.it");

        OnboardingSelfCareMessage inputMessage = new OnboardingSelfCareMessage();
        inputMessage.setId("7014954b-5a2f-4aed-9f26-b2b778c2a126");
        inputMessage.setBilling(new OnboardingSelfCareMessage.Billing());
        inputMessage.setContentType("application/octet-stream");
        inputMessage.setInstitution(institution);
        inputMessage.setProduct("prod-pn-dev");
        inputMessage.setInternalIstitutionID("8861b02d-8cb4-4de9-95d2-5ed02f3de38a");
        inputMessage.setState("ACTIVE");
        inputMessage.setUpdatedAt(Instant.parse("2023-01-10T15:20:38.94Z"));
        inputMessage.setCreatedAt(Instant.parse("2023-01-05T13:41:30.621Z"));
        inputMessage.setZipCode("02045");

        return inputMessage;
    }

}
