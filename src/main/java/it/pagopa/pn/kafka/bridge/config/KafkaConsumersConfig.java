package it.pagopa.pn.kafka.bridge.config;

import lombok.CustomLog;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@CustomLog
public class KafkaConsumersConfig {

    @Bean
    @ConfigurationProperties("pn.kafka-bridge.onboarding-contracts")
    public KafkaClusterProperties onboardingContractsKafkaProps() {
        KafkaClusterProperties properties = new KafkaClusterProperties();
        log.info("Onboarding Contracts Kafka Properties: {}", properties);
        return properties;
    }

    @Bean
    @ConfigurationProperties("pn.kafka-bridge.delegations")
    public KafkaClusterProperties delegationsKafkaProps() {
        KafkaClusterProperties properties = new KafkaClusterProperties();
        log.info("Delegations Kafka Properties: {}", properties);
        return properties;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> onboardingContractsKafkaListenerContainerFactory(
            @Qualifier("onboardingContractsKafkaProps") KafkaClusterProperties p) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory(p));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> delegationsKafkaListenerContainerFactory(
            @Qualifier("delegationsKafkaProps") KafkaClusterProperties p,
            @Value("${pn.kafka-bridge.delegations.auth-exception-retry-interval:60s}") Duration authExceptionRetryInterval) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory(p));
        factory.getContainerProperties().setAuthExceptionRetryInterval(authExceptionRetryInterval);
        return factory;
    }

    private static <V> DefaultKafkaConsumerFactory<String, V> consumerFactory(KafkaClusterProperties p) {
        Map<String, Object> cfg = new HashMap<>(p.getProperties());
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers());
        return new DefaultKafkaConsumerFactory<>(cfg);
    }
}
