package it.pagopa.pn.kafka.bridge.util;

import de.neuland.assertj.logging.ExpectedLogging;
import de.neuland.assertj.logging.ExpectedLoggingAssertions;
import it.pagopa.pn.kafka.bridge.middleware.kafka.consumer.OnboardingSelfCareConsumer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsOnboardingProducer;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ValidatorUtilTest {

    @MockitoBean
    private OnboardingSelfCareConsumer consumer;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private SqsOnboardingProducer sqsOnboardingProducer;

    @RegisterExtension
    private final ExpectedLogging logging = ExpectedLogging.forSource(ValidatorUtil.class);

    @Autowired
    private ValidatorUtil validatorUtil;

    @Test
    void testCFOKValidation() {
        String cf = "CLMCST42R12D969Z";
        OnboardingSelfCareMessage message = new OnboardingSelfCareMessage();
        OnboardingSelfCareMessage.Institution institution = new OnboardingSelfCareMessage.Institution();
        institution.setTaxCode(cf);
        message.setInstitution(institution);

        assertThat(validatorUtil.getValidator().validate(message)).isEmpty();
    }

    @Test
    void testTaxCodePartitaIvaOKValidation() {
        String pi = "01207650639";
        OnboardingSelfCareMessage message = new OnboardingSelfCareMessage();
        OnboardingSelfCareMessage.Institution institution = new OnboardingSelfCareMessage.Institution();
        institution.setTaxCode(pi);
        message.setInstitution(institution);

        assertThat(validatorUtil.getValidator().validate(message)).isEmpty();
    }

    @Test
    void testTaxCodePartitaIvaInvalidValidation() {
        String pi = "01207650639,";
        OnboardingSelfCareMessage message = new OnboardingSelfCareMessage();
        var institution = new OnboardingSelfCareMessage.Institution();
        institution.setTaxCode(pi);
        message.setInstitution(institution);

        assertThat(validatorUtil.getValidator().validate(message)).hasSize(1);
    }

    @Test
    void testTaxCodeNullValidation() {
        OnboardingSelfCareMessage message = new OnboardingSelfCareMessage();
        var institution = new OnboardingSelfCareMessage.Institution();
        institution.setTaxCode(null);
        message.setInstitution(institution);

        assertThat(validatorUtil.getValidator().validate(message)).hasSize(1);
    }

    @Test
    void isValidDelegationSelfCareMessageCompleteMessageReturnsTrueTest() {
        boolean actual = validatorUtil.isValidDelegationSelfCareMessage(completeDelegation());

        assertThat(actual).isTrue();
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    @Test
    void isValidDelegationSelfCareMessageWithoutOptionalFieldsReturnsTrueTest() {
        DelegationSelfCareMessage message = completeDelegation();
        message.setIsTest(null);
        message.setEventType(null);
        message.setUpdatedAt(null);
        message.setClosedAt(null);
        message.setInstitutionFromRootName(null);

        boolean actual = validatorUtil.isValidDelegationSelfCareMessage(message);

        assertThat(actual).isTrue();
        ExpectedLoggingAssertions.assertThat(logging).hasNoErrorMessage();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("delegationMandatoryFieldRemovers")
    void isValidDelegationSelfCareMessageMissingMandatoryFieldReturnsFalseAndLogsErrorTest(String field, Consumer<DelegationSelfCareMessage> remover) {
        DelegationSelfCareMessage message = completeDelegation();
        remover.accept(message);

        boolean actual = validatorUtil.isValidDelegationSelfCareMessage(message);

        assertThat(actual).isFalse();
        ExpectedLoggingAssertions.assertThat(logging).hasErrorMessageMatching(".*Payload received is not valid.*" + field + ".*");
    }

    @Test
    void isValidDelegationSelfCareMessageBlankMandatoryFieldReturnsFalseTest() {
        DelegationSelfCareMessage message = completeDelegation();
        message.setDelegationId(" ");

        boolean actual = validatorUtil.isValidDelegationSelfCareMessage(message);

        assertThat(actual).isFalse();
        ExpectedLoggingAssertions.assertThat(logging).hasErrorMessageMatching(".*Payload received is not valid.*delegationId.*");
    }

    private static Stream<Arguments> delegationMandatoryFieldRemovers() {
        return Stream.of(
                Arguments.of("id", (Consumer<DelegationSelfCareMessage>) m -> m.setId(null)),
                Arguments.of("delegationId", (Consumer<DelegationSelfCareMessage>) m -> m.setDelegationId(null)),
                Arguments.of("from", (Consumer<DelegationSelfCareMessage>) m -> m.setFrom(null)),
                Arguments.of("institutionFromName", (Consumer<DelegationSelfCareMessage>) m -> m.setInstitutionFromName(null)),
                Arguments.of("institutionToName", (Consumer<DelegationSelfCareMessage>) m -> m.setInstitutionToName(null)),
                Arguments.of("to", (Consumer<DelegationSelfCareMessage>) m -> m.setTo(null)),
                Arguments.of("toTaxCode", (Consumer<DelegationSelfCareMessage>) m -> m.setToTaxCode(null)),
                Arguments.of("fromTaxCode", (Consumer<DelegationSelfCareMessage>) m -> m.setFromTaxCode(null)),
                Arguments.of("toType", (Consumer<DelegationSelfCareMessage>) m -> m.setToType(null)),
                Arguments.of("fromType", (Consumer<DelegationSelfCareMessage>) m -> m.setFromType(null)),
                Arguments.of("productId", (Consumer<DelegationSelfCareMessage>) m -> m.setProductId(null)),
                Arguments.of("type", (Consumer<DelegationSelfCareMessage>) m -> m.setType(null)),
                Arguments.of("status", (Consumer<DelegationSelfCareMessage>) m -> m.setStatus(null)),
                Arguments.of("createdAt", (Consumer<DelegationSelfCareMessage>) m -> m.setCreatedAt(null))
        );
    }

    private DelegationSelfCareMessage completeDelegation() {
        DelegationSelfCareMessage message = new DelegationSelfCareMessage();
        message.setId("0f3a9a52-5a3b-4d0c-9f0e-0d8a5d6f1a11");
        message.setDelegationId("6a1b6a4e-2f1d-4c0e-8d55-3b7f0c1e9a22");
        message.setFrom("b1f3c2d4-0a1b-4c2d-8e3f-4a5b6c7d8e9f");
        message.setInstitutionFromName("Comune di Milano");
        message.setInstitutionFromRootName("Comune di Milano Root");
        message.setInstitutionToName("Partner Tecnologico Srl");
        message.setTo("c2a4d3e5-1b2c-4d3e-9f40-5b6c7d8e9f01");
        message.setToTaxCode("01234567890");
        message.setFromTaxCode("00488410010");
        message.setToType("PT");
        message.setFromType("PA");
        message.setProductId("prod-pn-dev");
        message.setType("PT");
        message.setStatus("ACTIVE");
        message.setCreatedAt(Instant.parse("2024-07-09T12:40:21.414946089Z"));
        message.setUpdatedAt(Instant.parse("2024-08-10T09:15:00.123Z"));
        message.setClosedAt(Instant.parse("2024-09-11T07:05:30Z"));
        message.setIsTest(false);
        message.setEventType("ADD");
        return message;
    }
}
