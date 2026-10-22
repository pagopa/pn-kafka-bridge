package it.pagopa.pn.kafka.bridge.middleware.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

final class PaymentFlowFixtures {

    static final String TOPIC = "payment-events";
    static final Instant PAYMENT_TIMESTAMP = Instant.parse("2026-10-20T08:00:00Z");
    static final Instant PUBLISHED_TIMESTAMP = Instant.parse("2026-10-20T08:01:00Z");
    static final int MAX_BATCH_BYTES = 1_048_576;

    private PaymentFlowFixtures() {
    }

    static String validPayment(int index) {
        return """
                {
                  "id": "payment-%d",
                  "timestamp": "2026-10-20T08:00:00Z",
                  "debtorPosition": {
                    "modelType": "1", "iuv": "iuv-%d", "iur": "iur-%d",
                    "noticeNumber": "notice-%d"
                  },
                  "creditor": {"idPA": "12345678901", "companyName": "Test creditor"},
                  "paymentInfo": {
                    "dueDate": "2026-10-21T00:00:00Z", "totalNotice": "2",
                    "paymentChannel": "PSP", "amount": 1000, "fee": 50
                  }
                }
                """.formatted(index, index, index, index);
    }

    static ConsumerRecord<String, String> record(int index, String body) {
        return new ConsumerRecord<>(TOPIC, 0, index, PUBLISHED_TIMESTAMP.plusMillis(index).toEpochMilli(),
                TimestampType.CREATE_TIME, -1, body.getBytes(StandardCharsets.UTF_8).length,
                "payment-key-" + index, body, new RecordHeaders(), Optional.empty());
    }
}
