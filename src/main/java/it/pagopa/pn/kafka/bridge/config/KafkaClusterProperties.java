package it.pagopa.pn.kafka.bridge.config;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class KafkaClusterProperties {
    private String bootstrapServers;
    private String groupId;
    private String topicName;
    private int concurrency = 1;
    private Map<String, String> properties = new HashMap<>();
}