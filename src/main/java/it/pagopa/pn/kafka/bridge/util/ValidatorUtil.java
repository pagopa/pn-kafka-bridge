package it.pagopa.pn.kafka.bridge.util;

import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.validation.*;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidatorUtil {

    private final Validator validator;


    public boolean isValidOnboardingSelfCareMessage(OnboardingSelfCareMessage inputMessage) {
        Set<ConstraintViolation<OnboardingSelfCareMessage>> validateResults = validator.validate(inputMessage);
        if(! CollectionUtils.isEmpty(validateResults)) {
            log.error("[{}] Payload received is not valid: {}", inputMessage.getId(), validateResults);
            return false;
        }
        return true;
    }

    protected Validator getValidator() {
        return validator;
    }
}
