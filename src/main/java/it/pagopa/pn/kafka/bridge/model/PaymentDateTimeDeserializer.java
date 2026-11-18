package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;

public class PaymentDateTimeDeserializer extends StdDeserializer<Instant> {

    private static final ZoneId ROME = ZoneId.of("Europe/Rome");
    private static final String EXPECTED_FORMAT = "Expected uuuu-MM-dd'T'HH:mm:ss.SSSSSS without offset";
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendPattern("-MM-dd'T'HH:mm:ss.SSSSSS")
            .toFormatter(Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    public PaymentDateTimeDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return context.reportInputMismatch(Instant.class, EXPECTED_FORMAT);
        }

        String value = parser.getText();
        try {
            return LocalDateTime.parse(value, FORMATTER).atZone(ROME).toInstant();
        } catch (DateTimeParseException e) {
            throw context.weirdStringException(value, Instant.class, EXPECTED_FORMAT);
        }
    }
}
