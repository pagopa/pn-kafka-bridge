package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpochMillisInstantDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void interpretsIntegerTimestampAsMillisecondsRegardlessOfMapperSetting() throws Exception {
        String input = "{\"timestamp\":1794929244155}";
        Instant expected = Instant.ofEpochMilli(1794929244155L);

        assertEquals(expected, objectMapper.readValue(input, PagoPaPaymentMessage.class).getTimestamp());
        objectMapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        assertEquals(expected, objectMapper.readValue(input, PagoPaPaymentMessage.class).getTimestamp());
    }

    @Test
    void preservesIsoTimestampAndNull() throws Exception {
        assertEquals(Instant.parse("2026-10-20T08:00:00Z"),
                objectMapper.readValue("{\"timestamp\":\"2026-10-20T08:00:00Z\"}",
                        PagoPaPaymentMessage.class).getTimestamp());
        assertNull(objectMapper.readValue("{\"timestamp\":null}", PagoPaPaymentMessage.class).getTimestamp());
    }

    @Test
    void rejectsIntegersOutsideLongRange() {
        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(
                "{\"timestamp\":9223372036854775808}", PagoPaPaymentMessage.class));
    }
}
