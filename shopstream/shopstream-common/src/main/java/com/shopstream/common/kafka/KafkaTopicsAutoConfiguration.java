package com.shopstream.common.kafka;

import com.shopstream.common.events.Topics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Declares every ShopStream topic. Spring's KafkaAdmin creates them on startup
 * if they do not exist yet (and does nothing if they do), so it does not
 * matter which service starts first.
 *
 * Partitions are the unit of parallelism in Kafka: with 3 partitions, up to 3
 * instances of a consumer group can read a topic in parallel. Events for the
 * same order use the orderId as the message key, so they always land in the
 * same partition and are read in the order they were written.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaAdmin.class)
public class KafkaTopicsAutoConfiguration {

    @Bean
    public KafkaAdmin.NewTopics shopstreamTopics(@Value("${app.kafka.partitions:3}") int partitions,
                                                 @Value("${app.kafka.replication-factor:1}") int replicationFactor) {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(Topics.ORDER_EVENTS).partitions(partitions).replicas(replicationFactor).build(),
                TopicBuilder.name(Topics.INVENTORY_EVENTS).partitions(partitions).replicas(replicationFactor).build(),
                TopicBuilder.name(Topics.PAYMENT_EVENTS).partitions(partitions).replicas(replicationFactor).build(),
                TopicBuilder.name(Topics.PRODUCT_EVENTS).partitions(partitions).replicas(replicationFactor).build());
    }
}
