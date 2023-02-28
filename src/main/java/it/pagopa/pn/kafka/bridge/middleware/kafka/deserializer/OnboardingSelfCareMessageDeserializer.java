package it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

@Slf4j
public class OnboardingSelfCareMessageDeserializer implements Deserializer<OnboardingSelfCareMessage> {

    private final ObjectMapper objectMapper;

    public OnboardingSelfCareMessageDeserializer() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        objectMapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer,
                                                 Object beanOrClass, String propertyName) throws IOException {

                log.warn("Unknown property {} encountered while deserialization JSON with value: {}", propertyName, p.readValueAsTree());
                return true;
            }
        });
    }

    @Override
    public OnboardingSelfCareMessage deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
               log.warn("Null received at deserializing");
                return null;
            }
            log.trace("Deserializing from topic: {}...", topic);
            return objectMapper.readValue(data, OnboardingSelfCareMessage.class);
        } catch (Exception e) {
            log.error("Error when deserializing byte[] to OnboardingSelfCareMessage", e);
            return null; //il filtro scarta sia i null che i product non PN
        }
    }


}
