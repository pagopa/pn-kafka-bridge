package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EuroCentDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void convertsDecimalAmountsToCents() throws Exception {
        PagoPaPaymentMessage message = objectMapper.readValue(
                "{\"amount\":\"50.35\",\"fee\":1.25}", PagoPaPaymentMessage.class);

        assertEquals(5035, message.getAmount());
        assertEquals(125, message.getFee());
    }

    @Test
    void convertsWholeAmountsAndPreservesNullFee() throws Exception {
        PagoPaPaymentMessage message = objectMapper.readValue(
                "{\"amount\":50,\"fee\":null}", PagoPaPaymentMessage.class);

        assertEquals(5000, message.getAmount());
        assertNull(message.getFee());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"0.001\"", "\"21474836.48\"", "\"invalid\"", "true", "{}"})
    void rejectsAmountsNotRepresentableInIntegerCents(String input) {
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(
                "{\"amount\":" + input + "}", PagoPaPaymentMessage.class));
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(
                "{\"fee\":" + input + "}", PagoPaPaymentMessage.class));
    }
}
