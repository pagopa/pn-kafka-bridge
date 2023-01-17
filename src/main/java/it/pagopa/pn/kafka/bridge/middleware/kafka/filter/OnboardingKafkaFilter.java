package it.pagopa.pn.kafka.bridge.middleware.kafka.filter;

import it.pagopa.pn.kafka.bridge.config.PnKafkaBridgeConfig;
import it.pagopa.pn.kafka.bridge.model.OnboardingSelfCareMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnboardingKafkaFilter implements RecordFilterStrategy<Object, OnboardingSelfCareMessage> {

    private final PnKafkaBridgeConfig config;

    private boolean filterEnabled;

    @PostConstruct
    public void init() {
        String selfcarePnProductId = config.getSelfcarePnProductId();
        this.filterEnabled = StringUtils.hasText(selfcarePnProductId);
        log.info("Filter enabled for onboarding: {} with productId: {}", filterEnabled, selfcarePnProductId);
    }


    @Override
    public boolean filter(ConsumerRecord<Object, OnboardingSelfCareMessage> consumerRecord) {
        boolean discarded;
        if(! filterEnabled) {
            discarded = false;
        }
        else {
            discarded = consumerRecord.value() == null
                    ||
                    (! consumerRecord.value().getProduct().equals(config.getSelfcarePnProductId()) );
        }

        if(discarded) {
            log.trace("Record discarded during filtering: {}", consumerRecord.value());
        }

        return discarded;
    }


}
