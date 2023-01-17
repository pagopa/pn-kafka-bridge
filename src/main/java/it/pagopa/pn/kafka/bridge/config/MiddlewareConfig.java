package it.pagopa.pn.kafka.bridge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsOnboardingProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class MiddlewareConfig {

    @Bean
    SqsOnboardingProducer sqsNotificationPaidProducer(SqsClient sqs, ObjectMapper objMapper, PnKafkaBridgeConfig cfg) {
        return new SqsOnboardingProducer( sqs, cfg.getSqsTopics().getOnboardingInput(), objMapper);
    }
}
