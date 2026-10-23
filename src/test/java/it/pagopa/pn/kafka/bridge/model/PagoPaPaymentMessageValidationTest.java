package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
            "id, 20",
            "debtorPosition.modelType, 1000",
            "debtorPosition.iuv, 1000",
            "debtorPosition.iur, 1000",
            "debtorPosition.noticeNumber, 1000",
            "creditor.idPA, 1000",
            "creditor.companyName, 1000",
            "paymentInfo.totalNotice, 1000",
            "paymentInfo.paymentChannel, 1000"
    })
    void enforcesStringLengthThroughCascadedValidation(String property, int maxLength) {
        PagoPaPaymentMessage message = validMessage();
        setString(message, property, "x".repeat(maxLength));

        assertThat(VALIDATOR.validate(message)).isEmpty();

        setString(message, property, "x".repeat(maxLength + 1));

        assertThat(VALIDATOR.validate(message))
                .singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo(property));
    }

    private static PagoPaPaymentMessage validMessage() {
        PagoPaPaymentMessage message = new PagoPaPaymentMessage();
        message.setId("payment-id");
        DebtorPosition debtorPosition = new DebtorPosition();
        debtorPosition.setNoticeNumber("notice");
        message.setDebtorPosition(debtorPosition);
        Creditor creditor = new Creditor();
        creditor.setIdPA("12345678901");
        message.setCreditor(creditor);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentDateTime(Instant.parse("2026-10-20T08:00:00Z"));
        paymentInfo.setAmount(1000);
        message.setPaymentInfo(paymentInfo);
        return message;
    }

    private static void setString(PagoPaPaymentMessage message, String property, String value) {
        switch (property) {
            case "id" -> message.setId(value);
            case "debtorPosition.modelType" -> message.getDebtorPosition().setModelType(value);
            case "debtorPosition.iuv" -> message.getDebtorPosition().setIuv(value);
            case "debtorPosition.iur" -> message.getDebtorPosition().setIur(value);
            case "debtorPosition.noticeNumber" -> message.getDebtorPosition().setNoticeNumber(value);
            case "creditor.idPA" -> message.getCreditor().setIdPA(value);
            case "creditor.companyName" -> message.getCreditor().setCompanyName(value);
            case "paymentInfo.totalNotice" -> message.getPaymentInfo().setTotalNotice(value);
            case "paymentInfo.paymentChannel" -> message.getPaymentInfo().setPaymentChannel(value);
            default -> throw new IllegalArgumentException("Unknown payment property: " + property);
        }
    }
}
