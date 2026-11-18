package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.BigDecimal;

public class EuroCentDeserializer extends StdDeserializer<Integer> {

    public EuroCentDeserializer() {
        super(Integer.class);
    }

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token != JsonToken.VALUE_STRING && token != JsonToken.VALUE_NUMBER_INT
                && token != JsonToken.VALUE_NUMBER_FLOAT) {
            return context.reportInputMismatch(Integer.class, "Expected an amount in euros");
        }

        String value = parser.getText();
        try {
            return new BigDecimal(value).movePointRight(2).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw context.weirdStringException(value, Integer.class,
                    "Expected an amount in euros convertible exactly to integer cents");
        }
    }
}
