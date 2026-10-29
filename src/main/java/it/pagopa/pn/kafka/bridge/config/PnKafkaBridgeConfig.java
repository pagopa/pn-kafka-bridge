package it.pagopa.pn.kafka.bridge.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "pn.kafka-bridge")
@Slf4j
@Data
@ToString
@Import(SharedAutoConfiguration.class)
public class PnKafkaBridgeConfig {

    private String selfcarePnProductId;

    private Onboarding onboarding = new Onboarding();

    private Delegations delegations = new Delegations();

    @Data
    public static class Onboarding {
        private String topicName;
        private String groupId;
        private String queueName;
        @ToString.Exclude
        private String saslJaasPassword;
    }

    @Data
    public static class Delegations {
        private String topicName;
        private String groupId;
        private String queueName;
        @ToString.Exclude
        private String saslJaasPassword;
    }

}
