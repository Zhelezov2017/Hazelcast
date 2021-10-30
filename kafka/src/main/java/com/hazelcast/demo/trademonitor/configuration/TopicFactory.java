package com.hazelcast.demo.trademonitor.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

import static com.hazelcast.platform.demos.banking.trademonitor.MyConstants.KAFKA_TOPIC_NAME_TRADES;

@Component
public class TopicFactory {

    @Bean
    public NewTopic createTopic() {
        return TopicBuilder.name(KAFKA_TOPIC_NAME_TRADES)
            .partitions(271)
            .replicas(1)
            .build();
    }
}
