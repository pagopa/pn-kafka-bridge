package it.pagopa.pn.kafka.bridge.middleware.kafka.filter;

import it.pagopa.pn.kafka.bridge.config.PnKafkaBridgeConfig;
import it.pagopa.pn.kafka.bridge.model.DelegationSelfCareMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DelegationsKafkaFilterTest {

    private static final String PRODUCT_ID = "prod-pn-dev";

    @Test
    void filterPtDelegationOfSendProductNotDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter(PRODUCT_ID);

        assertThat(filter.filter(record(message("PT", PRODUCT_ID)))).isFalse();
    }

    @Test
    void filterNullValueDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter(PRODUCT_ID);

        assertThat(filter.filter(record(null))).isTrue();
    }

    @Test
    void filterAooDelegationDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter(PRODUCT_ID);

        assertThat(filter.filter(record(message("AOO", PRODUCT_ID)))).isTrue();
    }

    @Test
    void filterEaDelegationDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter(PRODUCT_ID);

        assertThat(filter.filter(record(message("EA", PRODUCT_ID)))).isTrue();
    }

    @Test
    void filterNullTypeDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter(PRODUCT_ID);

        assertThat(filter.filter(record(message(null, PRODUCT_ID)))).isTrue();
    }

    @Test
    void filterPtDelegationOfOtherProductDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter(PRODUCT_ID);

        assertThat(filter.filter(record(message("PT", "prod-io")))).isTrue();
    }

    @Test
    void filterEmptyConfiguredProductProductFilterDisabledTest() {
        DelegationsKafkaFilter filter = buildFilter("");

        assertThat(filter.filter(record(message("PT", "prod-io")))).isFalse();
    }

    @Test
    void filterNullConfiguredProductProductFilterDisabledTest() {
        DelegationsKafkaFilter filter = buildFilter(null);

        assertThat(filter.filter(record(message("PT", "prod-io")))).isFalse();
    }

    @Test
    void filterEmptyConfiguredProductAndNotPtDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter("");

        assertThat(filter.filter(record(message("AOO", PRODUCT_ID)))).isTrue();
    }

    @Test
    void filterEmptyConfiguredProductAndNullValueDiscardedTest() {
        DelegationsKafkaFilter filter = buildFilter("");

        assertThat(filter.filter(record(null))).isTrue();
    }

    private DelegationsKafkaFilter buildFilter(String configuredProductId) {
        PnKafkaBridgeConfig config = new PnKafkaBridgeConfig();
        config.setSelfcarePnProductId(configuredProductId);
        DelegationsKafkaFilter filter = new DelegationsKafkaFilter(config);
        filter.init();
        return filter;
    }

    private DelegationSelfCareMessage message(String type, String productId) {
        DelegationSelfCareMessage message = new DelegationSelfCareMessage();
        message.setType(type);
        message.setProductId(productId);
        return message;
    }

    private ConsumerRecord<Object, DelegationSelfCareMessage> record(DelegationSelfCareMessage value) {
        return new ConsumerRecord<>("sc-delegations", 0, 0L, null, value);
    }
}
