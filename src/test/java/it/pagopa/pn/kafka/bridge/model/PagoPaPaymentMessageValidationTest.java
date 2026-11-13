package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PagoPaPaymentMessageValidationTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @ParameterizedTest
    @CsvSource({
            "modelType, 1000",
            "iuv, 1000",
            "iur, 1000",
            "noticeNumber, 1000",
            "idPA, 1000",
            "companyName, 1000",
            "paymentChannel, 1000",
            "touchpoint, 1000"
    })
    void enforcesStringLengthForFlatPaymentFields(String property, int maxLength) {
        PagoPaPaymentMessage message = validMessage();
        setString(message, property, "x".repeat(maxLength));

        assertThat(VALIDATOR.validate(message)).isEmpty();

        setString(message, property, "x".repeat(maxLength + 1));

        assertThat(VALIDATOR.validate(message))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo(property));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "2", "2147483647", "0002"})
    void acceptsOptionalTotalNoticeWithinIntegerRange(String totalNotice) {
        PagoPaPaymentMessage message = validMessage();
        message.setTotalNotice(totalNotice);

        assertThat(VALIDATOR.validate(message)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid", "2147483648", "-2147483649",
            "-1", "+1", "1.5", "1e2", " 2", "2 "})
    void rejectsMalformedTotalNoticeThroughCascadedValidation(String totalNotice) {
        PagoPaPaymentMessage message = validMessage();
        message.setTotalNotice(totalNotice);

        assertThat(VALIDATOR.validate(message))
                .isNotEmpty()
                .allSatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .isEqualTo("totalNotice"));
    }

    @Test
    void enforcesTotalNoticeLengthWithoutViolatingNumericConstraints() {
        PagoPaPaymentMessage message = validMessage();
        message.setTotalNotice("0".repeat(999) + "2");

        assertThat(VALIDATOR.validate(message)).isEmpty();

        message.setTotalNotice("0".repeat(1000) + "2");

        assertThat(VALIDATOR.validate(message))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString())
                        .isEqualTo("totalNotice"));
    }

    private static PagoPaPaymentMessage validMessage() {
        PagoPaPaymentMessage message = new PagoPaPaymentMessage();
        message.setId("payment-id");
        message.setNoticeNumber("notice");
        message.setIdPA("12345678901");
        message.setPaymentDateTime(Instant.parse("2026-10-20T08:00:00Z"));
        message.setAmount(1000);
        return message;
    }

    private static void setString(PagoPaPaymentMessage message, String property, String value) {
        switch (property) {
            case "modelType" -> message.setModelType(value);
            case "iuv" -> message.setIuv(value);
            case "iur" -> message.setIur(value);
            case "noticeNumber" -> message.setNoticeNumber(value);
            case "idPA" -> message.setIdPA(value);
            case "companyName" -> message.setCompanyName(value);
            case "paymentChannel" -> message.setPaymentChannel(value);
            case "touchpoint" -> message.setTouchpoint(value);
            default -> throw new IllegalArgumentException("Unknown payment property: " + property);
        }
    }
}
