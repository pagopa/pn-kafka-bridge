package it.pagopa.pn.kafka.bridge.config;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.time.Duration;
import java.util.Map;

@Configuration
@CustomLog
@RequiredArgsConstructor
public class KafkaConsumersConfig {

    private static final String SASL_JAAS_CONFIG = "sasl.jaas.config";
    private static final String JAAS_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"%s\";";
    private final SslBundles sslBundles;
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> onboardingContractsKafkaListenerContainerFactory(PnKafkaBridgeConfig p) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory(p.getOnboardingContracts()));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> delegationsKafkaListenerContainerFactory(PnKafkaBridgeConfig p,
            @Value("${pn.kafka-bridge.delegations.auth-exception-retry-interval:60s}") Duration authExceptionRetryInterval) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory(p.getDelegations()));
        factory.getContainerProperties().setAuthExceptionRetryInterval(authExceptionRetryInterval);
        return factory;
    }

    private <V> DefaultKafkaConsumerFactory<String, V> consumerFactory(PnKafkaBridgeConfig.KafkaClusterProperties p) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties(sslBundles);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers());
        properties.put(SASL_JAAS_CONFIG, String.format(JAAS_TEMPLATE, p.getSaslJaasPassword()));
        properties.putAll(p.getProperties());
        return new DefaultKafkaConsumerFactory<>(properties);
    }

}