package it.pagopa.pn.kafka.bridge.mapper;

import it.pagopa.pn.api.dto.events.PnOnboardInstitutionPayload;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OnboardingMapper {


    public PnOnboardInstitutionPayload toPnOnboardInstitutionPayload(OnboardingSelfCareMessage onBoardingSelfCareMessage) {
        PnOnboardInstitutionPayload entity = new PnOnboardInstitutionPayload();
        entity.setStatus(onBoardingSelfCareMessage.getState());
        entity.setLastUpdate(Instant.from(onBoardingSelfCareMessage.getUpdatedAt()));
        entity.setTaxCode(onBoardingSelfCareMessage.getInstitution().getTaxCode());
        entity.setAddress(onBoardingSelfCareMessage.getInstitution().getAddress());
        entity.setDigitalAddress(onBoardingSelfCareMessage.getInstitution().getDigitalAddress());
        entity.setDescription(onBoardingSelfCareMessage.getInstitution().getDescription());
        entity.setId(onBoardingSelfCareMessage.getInternalIstitutionID());
        entity.setExternalId(onBoardingSelfCareMessage.getOnboardingTokenId());
        return entity;
    }
}
