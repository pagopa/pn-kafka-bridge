package it.pagopa.pn.kafka.bridge.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumersConfigTest {

    private static final String JAAS_PREFIX = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"";

    private final KafkaConsumersConfig config = new KafkaConsumersConfig(Mockito.mock(SslBundles.class), new KafkaProperties());

    @Test
    void delegationsKafkaListenerContainerFactoryUsesDelegationsPropertiesTest() {
        PnKafkaBridgeConfig bridgeConfig = new PnKafkaBridgeConfig();
        bridgeConfig.setDelegations(cluster("delegations-broker.example.com:9093", "delegations-secret"));
        bridgeConfig.getDelegations().setProperties(Map.of(
                "security.protocol", "SASL_SSL",
                "auto.offset.reset", "earliest"));

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                config.delegationsKafkaListenerContainerFactory(bridgeConfig, Duration.ofSeconds(60));

        Map<String, Object> consumerProperties = factory.getConsumerFactory().getConfigurationProperties();
        assertThat(consumerProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "delegations-broker.example.com:9093")
                .containsEntry("sasl.jaas.config", JAAS_PREFIX + "delegations-secret\";")
                .containsEntry("security.protocol", "SASL_SSL")
                .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
        assertThat(factory.getContainerProperties().getAuthExceptionRetryInterval()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void delegationsAndOnboardingContractsFactoriesAreIndependentTest() {
        PnKafkaBridgeConfig bridgeConfig = new PnKafkaBridgeConfig();
        bridgeConfig.setOnboardingContracts(cluster("onboarding-broker.example.com:9093", "onboarding-secret"));
        bridgeConfig.setDelegations(cluster("delegations-broker.example.com:9093", "delegations-secret"));

        Map<String, Object> onboardingProperties = config.onboardingContractsKafkaListenerContainerFactory(bridgeConfig)
                .getConsumerFactory().getConfigurationProperties();
        Map<String, Object> delegationsProperties = config.delegationsKafkaListenerContainerFactory(bridgeConfig, Duration.ofSeconds(60))
                .getConsumerFactory().getConfigurationProperties();

        assertThat(onboardingProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "onboarding-broker.example.com:9093")
                .containsEntry("sasl.jaas.config", JAAS_PREFIX + "onboarding-secret\";");
        assertThat(delegationsProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "delegations-broker.example.com:9093")
                .containsEntry("sasl.jaas.config", JAAS_PREFIX + "delegations-secret\";");
    }

    @Test
    void delegationsPropertiesBoundFromEnvironmentVariablesTest() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                "PN_KAFKA_BRIDGE_DELEGATIONS_TOPIC_NAME", "sc-delegations",
                "PN_KAFKA_BRIDGE_DELEGATIONS_GROUP_ID", "pn-kafka-bridge-delegations-dev",
                "PN_KAFKA_BRIDGE_DELEGATIONS_BOOTSTRAP_SERVERS", "delegations-broker.example.com:9093",
                "PN_KAFKA_BRIDGE_DELEGATIONS_PROPERTIES_AUTO_COMMIT_INTERVAL_MS", "2000",
                "KAFKA_DELEGATIONS_SASL_JAAS_PASSWORD", "delegations-secret")));

        PnKafkaBridgeConfig.KafkaClusterProperties properties = Binder.get(environment)
                .bind("pn.kafka-bridge.delegations", PnKafkaBridgeConfig.KafkaClusterProperties.class)
                .get();

        assertThat(properties.getTopicName()).isEqualTo("sc-delegations");
        assertThat(properties.getGroupId()).isEqualTo("pn-kafka-bridge-delegations-dev");
        assertThat(properties.getBootstrapServers()).isEqualTo("delegations-broker.example.com:9093");
        assertThat(properties.getSaslJaasPassword()).isEqualTo("delegations-secret");
        assertThat(properties.getProperties())
                .containsEntry(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "2000")
                .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer.DelegationSelfCareMessageDeserializer");
    }

    @Test
    void delegationsAutoCommitIntervalDefaultTest() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));

        PnKafkaBridgeConfig.KafkaClusterProperties properties = Binder.get(environment)
                .bind("pn.kafka-bridge.delegations", PnKafkaBridgeConfig.KafkaClusterProperties.class)
                .get();

        assertThat(properties.getProperties()).containsEntry(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    }

    private PnKafkaBridgeConfig.KafkaClusterProperties cluster(String bootstrapServers, String saslJaasPassword) {
        PnKafkaBridgeConfig.KafkaClusterProperties properties = new PnKafkaBridgeConfig.KafkaClusterProperties();
        properties.setBootstrapServers(bootstrapServers);
        properties.setSaslJaasPassword(saslJaasPassword);
        return properties;
    }
}
