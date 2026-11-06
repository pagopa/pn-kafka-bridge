package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import it.pagopa.pn.kafka.bridge.model.PagoPaPaymentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.validation.*;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidatorUtil {

    public static final String PAYLOAD_RECEIVED_IS_NOT_VALID = "[{}] Payload received is not valid: {}";
    private final Validator validator;

    protected Validator getValidator() {
        return validator;
    }

    public boolean isValidOnboardingSelfCareMessage(OnboardingSelfCareMessage inputMessage) {
        Set<ConstraintViolation<OnboardingSelfCareMessage>> validateResults = validator.validate(inputMessage);
        if(! CollectionUtils.isEmpty(validateResults)) {
            log.error(PAYLOAD_RECEIVED_IS_NOT_VALID, inputMessage.getInternalIstitutionID(), validateResults);
            return false;
        }
        return true;
    }

    public boolean isValidPaymentMessage(PagoPaPaymentMessage message) {
        Set<ConstraintViolation<PagoPaPaymentMessage>> validateResults = validator.validate(message);
        if(! CollectionUtils.isEmpty(validateResults)) {
            log.error(PAYLOAD_RECEIVED_IS_NOT_VALID, message.getId(), validateResults);
            return false;
        }
        return true;
    }
}
