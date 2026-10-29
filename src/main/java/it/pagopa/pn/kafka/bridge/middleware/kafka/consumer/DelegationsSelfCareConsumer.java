package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.DelegationService;
import it.pagopa.pn.kafka.bridge.util.ValidatorUtil;
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
public class DelegationsSelfCareConsumer {

    private final DelegationService delegationService;

    private final ValidatorUtil validatorUtil;

    @KafkaListener(
            id = "delegationsSelfCareListener",
            groupId = "${pn.kafka-bridge.delegations.group-id}",
            topics = "${pn.kafka-bridge.delegations.topic-name}",
            containerFactory = "delegationsKafkaListenerContainerFactory",
            filter = "delegationsKafkaFilter",
            autoStartup = "${pn.kafka-bridge.delegations.enabled}",
            properties = {
                    "value.deserializer=it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer.DelegationSelfCareMessageDeserializer"
            }
    )
    public void listen(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Payload DelegationSelfCareMessage inputMessage) {
        log.info("[{}] Received message from topic: {}, with value: {}", inputMessage.getDelegationId(), topic, inputMessage);
        if(validatorUtil.isValidDelegationSelfCareMessage(inputMessage)) {
            delegationService.sendMessage(inputMessage);
        }
    }
}
