package it.pagopa.pn.kafka.bridge.middleware.kafka.filter;

import it.pagopa.pn.kafka.bridge.config.PnKafkaBridgeConfig;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import jakarta.annotation.PostConstruct;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@CustomLog
public class DelegationsKafkaFilter implements RecordFilterStrategy<Object, DelegationSelfCareMessage> {

    private static final String PT_DELEGATION_TYPE = "PT";

    private final PnKafkaBridgeConfig config;

    private boolean productFilterEnabled;

    @PostConstruct
    public void init() {
        String selfcarePnProductId = config.getSelfcarePnProductId();
        this.productFilterEnabled = StringUtils.hasText(selfcarePnProductId);
        log.info("Product filter enabled for delegations: {} with productId: {}", productFilterEnabled, selfcarePnProductId);
    }

    @Override
    public boolean filter(ConsumerRecord<Object, DelegationSelfCareMessage> consumerRecord) {
        DelegationSelfCareMessage value = consumerRecord.value();
        boolean discarded = value == null
                || ! PT_DELEGATION_TYPE.equals(value.getType())
                || (productFilterEnabled && ! config.getSelfcarePnProductId().equals(value.getProductId()));

        if(discarded) {
            log.debug("Record discarded during filtering: {}", value);
        }

        return discarded;
    }
}
