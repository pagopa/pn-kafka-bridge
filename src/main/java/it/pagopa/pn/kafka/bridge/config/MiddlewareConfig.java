package it.pagopa.pn.kafka.bridge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsErrorEventProducer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsNoticePaymentProducer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsOnboardingProducer;
import it.pagopa.pn.kafka.bridge.middleware.queue.producer.sqs.SqsPaymentsToVerifyProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class MiddlewareConfig {

    @Bean
    SqsOnboardingProducer sqsNotificationPaidProducer(SqsClient sqs, ObjectMapper objMapper, PnKafkaBridgeConfig cfg) {
        return new SqsOnboardingProducer( sqs, cfg.getSqsTopics().getOnboardingInput(), objMapper);
    }

    @Bean
    SqsNoticePaymentProducer sqsNoticePaymentProducer(SqsClient sqs, ObjectMapper objMapper, PnKafkaBridgeConfig cfg) {
        return new SqsNoticePaymentProducer( sqs, cfg.getSqsTopics().getNoticePaymentInput(), objMapper);
    }

    @Bean
    SqsErrorEventProducer sqsErrorEventProducer(SqsClient sqs, ObjectMapper objMapper, PnKafkaBridgeConfig cfg) {
        return new SqsErrorEventProducer( sqs, cfg.getSqsTopics().getErrorEventInput(), objMapper);
    }

    @Bean
    SqsPaymentsToVerifyProducer sqsPaymentsToVerifyProducer(SqsClient sqs, ObjectMapper objMapper, PnKafkaBridgeConfig cfg) {
        return new SqsPaymentsToVerifyProducer( sqs, cfg.getSqsTopics().getPaymentsToVerify(), objMapper);
    }
}
