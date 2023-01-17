package it.pagopa.pn.kafka.bridge.service;

import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;

public interface OnboardingService {

    void sendMessage(OnboardingSelfCareMessage inputMessage);
}
