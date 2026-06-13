package com.medsimple.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Enables @Scheduled background workers (currently the media-processing poller).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
