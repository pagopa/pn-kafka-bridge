package it.pagopa.pn.kafka.bridge.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private static final String SASL_JAAS_CONFIG = "sasl.jaas.config";
    private static final String JAAS_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"%s\";";

    private final KafkaProperties kafkaProperties;

    private final PnKafkaBridgeConfig pnKafkaBridgeConfig;

    private final SslBundles sslBundles;

    Map<String, Object> onboardingConsumerProperties() {
        PnKafkaBridgeConfig.Onboarding onboarding = pnKafkaBridgeConfig.getOnboarding();
        return consumerProperties(onboarding.getGroupId(), onboarding.getSaslJaasPassword());
    }

    Map<String, Object> delegationsConsumerProperties() {
        PnKafkaBridgeConfig.Delegations delegations = pnKafkaBridgeConfig.getDelegations();
        return consumerProperties(delegations.getGroupId(), delegations.getSaslJaasPassword());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> onboardingKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer) {
        return buildContainerFactory(configurer, onboardingConsumerProperties());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> delegationsKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer) {
        return buildContainerFactory(configurer, delegationsConsumerProperties());
    }

    private ConcurrentKafkaListenerContainerFactory<Object, Object> buildContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer, Map<String, Object> properties) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        DefaultKafkaConsumerFactory<Object, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(properties);
        configurer.configure(factory, consumerFactory);
        return factory;
    }

    private Map<String, Object> consumerProperties(String groupId, String saslJaasPassword) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties(sslBundles);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(SASL_JAAS_CONFIG, String.format(JAAS_TEMPLATE, saslJaasPassword));
        return properties;
    }
}
