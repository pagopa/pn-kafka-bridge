package it.pagopa.pn.kafka.bridge.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "pn.kafka-bridge")
@Slf4j
@Data
@ToString
@Import(SharedAutoConfiguration.class)
public class PnKafkaBridgeConfig {

    private SqsTopics sqsTopics;

    private String selfcarePnProductId;

    private KafkaClusterProperties onboardingContracts;

    private KafkaClusterProperties delegations;

    private KafkaClusterProperties paymentEvents;

    @Data
    public static class SqsTopics {
        private String onboardingInput;
        private String delegationsInput;
        private String errorEventInput;
        private String kafkaBridgePayments;
        private String paymentsToVerify;
    }

    @Data
    public static class KafkaClusterProperties {
        private String bootstrapServers;
        private String groupId;
        private String topicName;
        private int concurrency = 1;
        private String saslJaasPassword;
        private Duration authExceptionRetryInterval;
        private Map<String, String> properties = new HashMap<>();
    }

    @PostConstruct
    public void logConfig() {
        log.info("Kafka Bridge configuration: {}", this);
    }

}
