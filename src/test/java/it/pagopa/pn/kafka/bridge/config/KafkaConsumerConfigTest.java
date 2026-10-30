package it.pagopa.pn.kafka.bridge.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerConfigTest {

    private static final String BOOTSTRAP = "broker.example.com:9093";
    private static final String JAAS_PREFIX = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"";

    @Mock
    private SslBundles sslBundles;

    @Mock
    private ConcurrentKafkaListenerContainerFactoryConfigurer configurer;

    private KafkaConsumerConfig config;

    @BeforeEach
    void init() {
        KafkaProperties kafkaProperties = new KafkaProperties();
        kafkaProperties.setBootstrapServers(List.of(BOOTSTRAP));
        kafkaProperties.getConsumer().setAutoOffsetReset("earliest");
        kafkaProperties.getConsumer().setEnableAutoCommit(true);
        kafkaProperties.getConsumer().setMaxPollRecords(250);
        kafkaProperties.getConsumer().getProperties().put("sasl.mechanism", "PLAIN");
        kafkaProperties.getConsumer().getProperties().put("security.protocol", "SASL_SSL");

        PnKafkaBridgeConfig bridgeConfig = new PnKafkaBridgeConfig();
        bridgeConfig.getOnboarding().setTopicName("sc-contracts");
        bridgeConfig.getOnboarding().setSaslJaasPassword("onboarding-secret");
        bridgeConfig.getDelegations().setTopicName("sc-delegations");
        bridgeConfig.getDelegations().setSaslJaasPassword("delegations-secret");

        config = new KafkaConsumerConfig(kafkaProperties, bridgeConfig, sslBundles);
    }

    @Test
    void onboardingConsumerPropertiesCommonPropertiesInheritedTest() {
        Map<String, Object> props = config.onboardingConsumerProperties();

        assertCommonProperties(props);
    }

    @Test
    void delegationsConsumerPropertiesCommonPropertiesInheritedTest() {
        Map<String, Object> props = config.delegationsConsumerProperties();

        assertCommonProperties(props);
    }

    @Test
    void consumerPropertiesBootstrapSharedBetweenFlowsTest() {
        Map<String, Object> onboarding = config.onboardingConsumerProperties();
        Map<String, Object> delegations = config.delegationsConsumerProperties();

        assertThat(onboarding.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
                .isEqualTo(delegations.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertThat(onboarding.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(List.of(BOOTSTRAP));
    }

    @Test
    void onboardingConsumerPropertiesJaasOfOnboardingWithoutGroupIdTest() {
        Map<String, Object> props = config.onboardingConsumerProperties();

        assertThat(props).doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
        assertThat(props.get("sasl.jaas.config")).isEqualTo(JAAS_PREFIX + "onboarding-secret\";");
    }

    @Test
    void delegationsConsumerPropertiesJaasOfDelegationsWithoutGroupIdTest() {
        Map<String, Object> props = config.delegationsConsumerProperties();

        assertThat(props).doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
        assertThat(props.get("sasl.jaas.config")).isEqualTo(JAAS_PREFIX + "delegations-secret\";");
    }

    @Test
    void consumerPropertiesDifferentJaasPerFlowTest() {
        Map<String, Object> onboarding = config.onboardingConsumerProperties();
        Map<String, Object> delegations = config.delegationsConsumerProperties();

        assertThat(onboarding.get("sasl.jaas.config")).isNotEqualTo(delegations.get("sasl.jaas.config"));
    }

    @Test
    void onboardingKafkaListenerContainerFactoryConfiguredWithOnboardingConsumerFactoryTest() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = config.onboardingKafkaListenerContainerFactory(configurer);

        assertThat(factory).isNotNull();
        Map<String, Object> props = captureConsumerFactoryProperties(factory);
        assertThat(props).doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
        assertThat(props.get("sasl.jaas.config")).isEqualTo(JAAS_PREFIX + "onboarding-secret\";");
        assertCommonProperties(props);
    }

    @Test
    void delegationsKafkaListenerContainerFactoryConfiguredWithDelegationsConsumerFactoryTest() {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = config.delegationsKafkaListenerContainerFactory(configurer);

        assertThat(factory).isNotNull();
        Map<String, Object> props = captureConsumerFactoryProperties(factory);
        assertThat(props).doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
        assertThat(props.get("sasl.jaas.config")).isEqualTo(JAAS_PREFIX + "delegations-secret\";");
        assertCommonProperties(props);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureConsumerFactoryProperties(ConcurrentKafkaListenerContainerFactory<Object, Object> factory) {
        ArgumentCaptor<ConsumerFactory<Object, Object>> captor = ArgumentCaptor.forClass(ConsumerFactory.class);
        Mockito.verify(configurer).configure(Mockito.same(factory), captor.capture());
        return captor.getValue().getConfigurationProperties();
    }

    private void assertCommonProperties(Map<String, Object> props) {
        assertThat(props.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo(List.of(BOOTSTRAP));
        assertThat(props.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)).isEqualTo("earliest");
        assertThat(props.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)).isEqualTo(true);
        assertThat(props.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG)).isEqualTo(250);
        assertThat(props.get("sasl.mechanism")).isEqualTo("PLAIN");
        assertThat(props.get("security.protocol")).isEqualTo("SASL_SSL");
    }
}
