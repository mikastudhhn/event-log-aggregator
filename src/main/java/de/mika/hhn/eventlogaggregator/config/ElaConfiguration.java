package de.mika.hhn.eventlogaggregator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main configuration class for Event Log Aggregator
 */
@Configuration
@EnableConfigurationProperties(ElaProperties.class)
@EnableScheduling
public class ElaConfiguration {
    
    // Additional configuration beans can be added here if needed
} 