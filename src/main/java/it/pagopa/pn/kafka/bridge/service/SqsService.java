package it.pagopa.pn.kafka.bridge.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
public class SqsService {
    private final SqsAsyncClient sqsClient;

    //FIXME correggere questo metodo secondo specifica SRS
    public Mono<SendMessageResponse> push(String msg, String pnNationalRegistriesCxId, String queueName, String eventType) {
        return Mono.fromFuture(() -> sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()))
                .flatMap(getQueueUrlResponse -> {
                    SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                            .queueUrl(getQueueUrlResponse.queueUrl())
                            .messageAttributes(buildMessageAttributeMap(pnNationalRegistriesCxId, eventType))
                            .messageBody(msg)
                            .build();
                    return Mono.fromFuture(() -> sqsClient.sendMessage(sendMsgRequest));
                });
    }

    private Map<String, MessageAttributeValue> buildMessageAttributeMap(String pnNationalRegistriesCxId, String eventType) {
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        if (StringUtils.hasText(pnNationalRegistriesCxId)) {
            attributes.put("clientId", MessageAttributeValue.builder().stringValue(pnNationalRegistriesCxId).dataType("String").build());
        }
        attributes.put("eventType", MessageAttributeValue.builder().stringValue(eventType).dataType("String").build());
        return attributes;
    }
}