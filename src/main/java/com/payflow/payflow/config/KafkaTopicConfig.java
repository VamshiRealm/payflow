package com.payflow.payflow.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name("payment.success").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("payment.failed").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentRefundTopic() {
        return TopicBuilder.name("payment.refund").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic fraudAlertTopic() {
        return TopicBuilder.name("fraud.alert").partitions(3).replicas(1).build();
    }
}