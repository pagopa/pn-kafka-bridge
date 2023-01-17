package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OnboardingSelfCareConsumer {

    private final OnboardingService onboardingService;

    @KafkaListener(
            id = "${pn.kafka-bridge.onboarding-group-id}",
            topics = "${pn.kafka-bridge.kafka-onboarding-topic-name}",
            filter = "onboardingKafkaFilter",
            properties = {
                    "value.deserializer=it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer.OnboardingSelfCareMessageDeserializer"
            }
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Payload OnboardingSelfCareMessage inputMessage) {
        log.info("[{}] Received message from topic: {}, with value: {}", inputMessage.getInternalIstitutionID(), topic, inputMessage);
        if(inputMessage != null) {
            onboardingService.sendMessage(inputMessage);
        }

    }

}
