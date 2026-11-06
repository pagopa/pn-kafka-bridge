package it.pagopa.pn.kafka.bridge.service;

import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;

public interface DelegationService {

    void sendMessage(DelegationSelfCareMessage inputMessage);
}
