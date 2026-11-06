package it.pagopa.pn.kafka.bridge.config;

import lombok.CustomLog;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

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
    @ConfigurationProperties("pn.kafka-bridge.payment-events")
    public KafkaClusterProperties paymentEventsKafkaProps() {
        KafkaClusterProperties properties = new KafkaClusterProperties();
        log.info("Payment Events Kafka Properties: {}", properties);
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
    public ConcurrentKafkaListenerContainerFactory<String, String> paymentEventsKafkaListenerContainerFactory(
            @Qualifier("paymentEventsKafkaProps") KafkaClusterProperties p) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory(p));
        factory.setBatchListener(true);
        factory.setConcurrency(p.getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(5_000L, FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }

    private static <V> DefaultKafkaConsumerFactory<String, V> consumerFactory(KafkaClusterProperties p) {
        Map<String, Object> cfg = new HashMap<>(p.getProperties());
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers());
        return new DefaultKafkaConsumerFactory<>(cfg);
    }
}