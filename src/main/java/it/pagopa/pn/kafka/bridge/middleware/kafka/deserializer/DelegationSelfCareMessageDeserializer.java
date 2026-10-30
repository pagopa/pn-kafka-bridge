package it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import lombok.CustomLog;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@CustomLog
public class DelegationSelfCareMessageDeserializer implements Deserializer<DelegationSelfCareMessage> {

    private static final String GENERIC_PRODUCT_PN = "prod-pn-";
    private static final Pattern PROD_PN_PATTERN;

    private final ObjectMapper objectMapper;

    static {
        String product = System.getenv("PN_KAFKA_BRIDGE_SELFCARE_PN_PRODUCT_ID");
        if(! StringUtils.hasText(product)) {
            product = GENERIC_PRODUCT_PN;
        }

        String regExp = "productId\" ?: ?\"" + product;
        log.info("RegExp to match in Deserializer: {}", regExp);
        PROD_PN_PATTERN = Pattern.compile(regExp);
    }

    public DelegationSelfCareMessageDeserializer() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public DelegationSelfCareMessage deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                log.warn("Null received at deserializing");
                return null;
            }
            log.trace("Deserializing from topic: {}...", topic);
            return objectMapper.readValue(data, DelegationSelfCareMessage.class);
        } catch (Exception e) {
            logException(data, e);
            return null;
        }
    }

    private void logException(byte[] data, Exception e) {
        String jsonString = new String(data, StandardCharsets.UTF_8);
        if(PROD_PN_PATTERN.matcher(jsonString).find()) {
            log.error("Error when deserializing byte[] to DelegationSelfCareMessage with input: {}", jsonString, e);
        }
        else {
            log.trace("Error when deserializing byte[] to DelegationSelfCareMessage with input: {}", jsonString, e);
        }
    }
}
