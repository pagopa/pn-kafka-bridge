package it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.regex.Pattern;

@Slf4j
public class OnboardingSelfCareMessageDeserializer implements Deserializer<OnboardingSelfCareMessage> {

    private static final Pattern PROD_PN_PATTERN = Pattern.compile("product\" ?: ?\"prod-pn-");
    private final ObjectMapper objectMapper;

    public OnboardingSelfCareMessageDeserializer() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
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
            logException(data, e);
            return null; //il filtro scarta sia i null che i product non PN
        }
    }

    //logghiamo ad error solo se il product è product-pn-<ENV>
    private void logException(byte[] data, Exception e) {
        String jsonString = new String(data);
        boolean isPnProduct = PROD_PN_PATTERN.matcher(jsonString).find();
        if(isPnProduct) {
            log.error(String.format("Error when deserializing byte[] to OnboardingSelfCareMessage with input: %s", jsonString), e);
        }
        else {
            log.warn(String.format("Error when deserializing byte[] to OnboardingSelfCareMessage with input: %s", jsonString), e);
        }
    }

}
