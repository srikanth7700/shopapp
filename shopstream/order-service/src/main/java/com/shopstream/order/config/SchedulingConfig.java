package com.shopstream.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods (used by the outbox publisher). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
