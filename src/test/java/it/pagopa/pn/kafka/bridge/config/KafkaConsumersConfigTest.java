package it.pagopa.pn.kafka.bridge.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumersConfigTest {

    private final KafkaConsumersConfig config = new KafkaConsumersConfig();

    @Test
    void delegationsKafkaListenerContainerFactoryUsesDelegationsPropertiesTest() {
        KafkaClusterProperties properties = new KafkaClusterProperties();
        properties.setBootstrapServers("delegations-broker.example.com:9093");
        properties.setProperties(Map.of(
                "security.protocol", "SASL_SSL",
                "sasl.jaas.config", "delegations-jaas",
                "auto.offset.reset", "earliest"));

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                config.delegationsKafkaListenerContainerFactory(properties, Duration.ofSeconds(60));

        Map<String, Object> consumerProperties = factory.getConsumerFactory().getConfigurationProperties();
        assertThat(consumerProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "delegations-broker.example.com:9093")
                .containsEntry("security.protocol", "SASL_SSL")
                .containsEntry("sasl.jaas.config", "delegations-jaas")
                .containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
                .doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
        assertThat(factory.getContainerProperties().getAuthExceptionRetryInterval()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void delegationsAndOnboardingContractsFactoriesAreIndependentTest() {
        KafkaClusterProperties onboarding = new KafkaClusterProperties();
        onboarding.setBootstrapServers("onboarding-broker.example.com:9093");
        onboarding.setProperties(Map.of("sasl.jaas.config", "onboarding-jaas"));
        KafkaClusterProperties delegations = new KafkaClusterProperties();
        delegations.setBootstrapServers("delegations-broker.example.com:9093");
        delegations.setProperties(Map.of("sasl.jaas.config", "delegations-jaas"));

        Map<String, Object> onboardingProperties = config.onboardingContractsKafkaListenerContainerFactory(onboarding)
                .getConsumerFactory().getConfigurationProperties();
        Map<String, Object> delegationsProperties = config.delegationsKafkaListenerContainerFactory(delegations, Duration.ofSeconds(60))
                .getConsumerFactory().getConfigurationProperties();

        assertThat(onboardingProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "onboarding-broker.example.com:9093")
                .containsEntry("sasl.jaas.config", "onboarding-jaas");
        assertThat(delegationsProperties)
                .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "delegations-broker.example.com:9093")
                .containsEntry("sasl.jaas.config", "delegations-jaas");
    }

    @Test
    void delegationsPropertiesBoundFromEnvironmentVariablesTest() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
                "PN_KAFKA_BRIDGE_DELEGATIONS_TOPIC_NAME", "sc-delegations",
                "PN_KAFKA_BRIDGE_DELEGATIONS_GROUP_ID", "pn-kafka-bridge-delegations-dev",
                "PN_KAFKA_BRIDGE_DELEGATIONS_BOOTSTRAP_SERVERS", "delegations-broker.example.com:9093",
                "PN_KAFKA_BRIDGE_DELEGATIONS_PROPERTIES_AUTO_COMMIT_INTERVAL_MS", "2000")));

        KafkaClusterProperties properties = Binder.get(environment)
                .bind("pn.kafka-bridge.delegations", KafkaClusterProperties.class)
                .get();

        assertThat(properties.getTopicName()).isEqualTo("sc-delegations");
        assertThat(properties.getGroupId()).isEqualTo("pn-kafka-bridge-delegations-dev");
        assertThat(properties.getBootstrapServers()).isEqualTo("delegations-broker.example.com:9093");
        assertThat(properties.getProperties())
                .containsEntry(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "2000")
                .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "it.pagopa.pn.kafka.bridge.middleware.kafka.deserializer.DelegationSelfCareMessageDeserializer");
    }

    @Test
    void delegationsAutoCommitIntervalDefaultTest() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));

        KafkaClusterProperties properties = Binder.get(environment)
                .bind("pn.kafka-bridge.delegations", KafkaClusterProperties.class)
                .get();

        assertThat(properties.getProperties()).containsEntry(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    }
}
