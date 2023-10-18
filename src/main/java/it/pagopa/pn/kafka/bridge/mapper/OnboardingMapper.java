package it.pagopa.pn.kafka.bridge.mapper;

import it.pagopa.pn.api.dto.events.PnOnboardInstitutionPayload;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import org.springframework.stereotype.Component;

@Component
public class OnboardingMapper {


    public PnOnboardInstitutionPayload toPnOnboardInstitutionPayload(OnboardingSelfCareMessage onBoardingSelfCareMessage) {
        PnOnboardInstitutionPayload entity = new PnOnboardInstitutionPayload();
        entity.setStatus(onBoardingSelfCareMessage.getState());
        entity.setCreated(onBoardingSelfCareMessage.getCreatedAt());
        entity.setLastUpdate(onBoardingSelfCareMessage.getUpdatedAt());
        entity.setTaxCode(onBoardingSelfCareMessage.getBilling().getVatNumber());
        entity.setAddress(onBoardingSelfCareMessage.getInstitution().getAddress());
        entity.setDigitalAddress(onBoardingSelfCareMessage.getInstitution().getDigitalAddress());
        entity.setDescription(onBoardingSelfCareMessage.getInstitution().getDescription());
        entity.setId(onBoardingSelfCareMessage.getInternalIstitutionID());
        entity.setExternalId(onBoardingSelfCareMessage.getOnboardingTokenId());
        entity.setZipCode(onBoardingSelfCareMessage.getZipCode());
        entity.setIpaCode(onBoardingSelfCareMessage.getInstitution().getOriginId());
        entity.setSdiCode(onBoardingSelfCareMessage.getBilling().getRecipientCode());

        if( onBoardingSelfCareMessage.getInstitution().getRootParent() == null ||
                onBoardingSelfCareMessage.getInstitution().getRootParent().getId() == null ) {

            entity.setRootId(onBoardingSelfCareMessage.getInternalIstitutionID());
            //if rootParentId is null rootDescription and RootIpaCode are not useful
            entity.setRootDescription(null);
            entity.setRootIpaCode(null);
        } else {
            entity.setRootId(onBoardingSelfCareMessage.getInstitution().getRootParent().getId());
            entity.setRootDescription(onBoardingSelfCareMessage.getInstitution().getRootParent().getDescription());
            entity.setRootIpaCode(onBoardingSelfCareMessage.getInstitution().getRootParent().getOriginId());
        }

        return entity;
    }
}
