package it.pagopa.pn.kafka.bridge.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ConfigurationProperties(prefix = "pn.kafka-bridge")
@Slf4j
@Data
@ToString
@Import(SharedAutoConfiguration.class)
public class PnKafkaBridgeConfig {

    private SqsTopics sqsTopics;

    private String selfcarePnProductId;


    @Data
    public static class SqsTopics {
        private String onboardingInput;
    }

}
