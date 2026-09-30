package com.shopstream.common.kafka;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * What happens when a @KafkaListener throws an exception?
 *
 * Without this, Spring Kafka retries a few times and then logs and skips the
 * message: the message is lost. With this handler:
 *   1. The listener is retried 3 times, 1 second apart (the error could be temporary, like a DB blip).
 *   2. If it still fails, the message goes to a "dead letter topic" named {@code <topic>.DLT}
 *      (for example {@code order-events.DLT}) so nothing is lost and you can inspect it in Kafka UI.
 *
 * It is registered as a Spring Boot auto-configuration (see
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports),
 * which is exactly how Spring Boot "starters" work: add the jar and the beans appear.
 * Spring Boot's Kafka listener container factory automatically picks up a
 * CommonErrorHandler bean.
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaOperations.class)
public class KafkaErrorHandlingAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaOperations) {
        // Partition -1 lets Kafka choose the DLT partition, so the DLT does not need
        // the same partition count as the original topic.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", -1));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.setRetryListeners((record, exception, deliveryAttempt) ->
                log.warn("Kafka listener failed (attempt {}) for topic={} key={}: {}",
                        deliveryAttempt, record.topic(), record.key(), exception.getMessage()));
        return handler;
    }
}
