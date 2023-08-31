package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.kafka.bridge.middleware.kafka.consumer.OnboardingSelfCareConsumer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsOnboardingProducer;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import it.pagopa.pn.kafka.bridge.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ValidatorUtilTest {

    @MockBean
    private OnboardingSelfCareConsumer consumer;

    @MockBean
    private OnboardingService onboardingService;

    @MockBean
    private SqsOnboardingProducer sqsOnboardingProducer;

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
        OnboardingSelfCareMessage.Billing billing = new OnboardingSelfCareMessage.Billing();
        billing.setVatNumber(pi);
        message.setBilling(billing);

        assertThat(validatorUtil.getValidator().validate(message)).hasSize(1);
    }

    @Test
    void testTaxCodeNullValidation() {
        OnboardingSelfCareMessage message = new OnboardingSelfCareMessage();
        OnboardingSelfCareMessage.Billing billing = new OnboardingSelfCareMessage.Billing();
        billing.setVatNumber(null);
        message.setBilling(billing);

        assertThat(validatorUtil.getValidator().validate(message)).hasSize(1);
    }
}
