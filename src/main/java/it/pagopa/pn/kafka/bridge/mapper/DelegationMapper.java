package it.pagopa.pn.kafka.bridge.mapper;

import it.pagopa.pn.api.dto.events.PnPtDelegationPayload;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import org.springframework.stereotype.Component;

@Component
public class DelegationMapper {

    public PnPtDelegationPayload toPnPtDelegationPayload(DelegationSelfCareMessage delegationSelfCareMessage) {
        PnPtDelegationPayload payload = new PnPtDelegationPayload();
        payload.setDelegationId(delegationSelfCareMessage.getDelegationId());
        payload.setDelegationType(delegationSelfCareMessage.getType());
        payload.setProductId(delegationSelfCareMessage.getProductId());
        payload.setStatus(delegationSelfCareMessage.getStatus());
        payload.setIsTest(delegationSelfCareMessage.getIsTest());
        payload.setSourceEventId(delegationSelfCareMessage.getId());
        payload.setSourceEventType(delegationSelfCareMessage.getEventType());
        payload.setCreatedAt(delegationSelfCareMessage.getCreatedAt());
        payload.setUpdatedAt(delegationSelfCareMessage.getUpdatedAt());
        payload.setClosedAt(delegationSelfCareMessage.getClosedAt());
        payload.setInstitutionId(delegationSelfCareMessage.getFrom());
        payload.setInstitutionName(delegationSelfCareMessage.getInstitutionFromName());
        payload.setInstitutionRootName(delegationSelfCareMessage.getInstitutionFromRootName());
        payload.setInstitutionTaxCode(delegationSelfCareMessage.getFromTaxCode());
        payload.setInstitutionType(delegationSelfCareMessage.getFromType());
        payload.setPtId(delegationSelfCareMessage.getTo());
        payload.setPtName(delegationSelfCareMessage.getInstitutionToName());
        payload.setPtTaxCode(delegationSelfCareMessage.getToTaxCode());
        payload.setPtType(delegationSelfCareMessage.getToType());
        return payload;
    }
}
