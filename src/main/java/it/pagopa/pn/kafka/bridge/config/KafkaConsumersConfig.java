package it.pagopa.pn.kafka.bridge.config;

import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import software.amazon.awssdk.core.exception.SdkClientException;

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
        factory.getContainerProperties().setAuthExceptionRetryInterval(p.getOnboardingContracts().getAuthExceptionRetryInterval());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> delegationsKafkaListenerContainerFactory(PnKafkaBridgeConfig p) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory(p.getDelegations()));
        factory.getContainerProperties().setAuthExceptionRetryInterval(p.getDelegations().getAuthExceptionRetryInterval());
        factory.setCommonErrorHandler(delegationsErrorHandler());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> paymentEventsKafkaListenerContainerFactory(PnKafkaBridgeConfig p) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory(p.getPaymentEvents()));
        factory.setBatchListener(true);
        factory.setConcurrency(p.getPaymentEvents().getConcurrency());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new FixedBackOff(5_000L, FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }

    private DefaultErrorHandler delegationsErrorHandler() {
        var handler = new DefaultErrorHandler(
                (rec, ex) -> log.fatal("Delegation event not delivered to SQS, manual resubmission required. topic={} partition={} offset={} value={}", rec.topic(), rec.partition(), rec.offset(), rec.value(), ex),
                new FixedBackOff(0L, 0L)
        );
        handler.setBackOffFunction((rec, ex) -> isSqsConnectionError(ex) ? new FixedBackOff(5_000L, FixedBackOff.UNLIMITED_ATTEMPTS) : null);
        return handler;
    }

    static boolean isSqsConnectionError(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof SdkClientException) {
                return true;
            }
        }
        return false;
    }

    private <V> DefaultKafkaConsumerFactory<String, V> consumerFactory(PnKafkaBridgeConfig.KafkaClusterProperties p) {
        Map<String, Object> properties = kafkaProperties.buildConsumerProperties(sslBundles);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers());
        properties.put(SASL_JAAS_CONFIG, String.format(JAAS_TEMPLATE, p.getSaslJaasPassword()));
        properties.putAll(p.getProperties());
        return new DefaultKafkaConsumerFactory<>(properties);
    }

}