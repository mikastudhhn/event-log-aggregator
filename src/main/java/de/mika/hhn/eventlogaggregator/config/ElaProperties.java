package de.mika.hhn.eventlogaggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.Duration;

@ConfigurationProperties(prefix = "ela")
public record ElaProperties(
    Aggregation aggregation,
    Directories directories,
    Scheduler scheduler,
    Sse sse
) {
    
    public record Aggregation(
        Windows windows
    ) {
        public record Windows(
            Duration hourly,
            Duration daily, 
            Duration weekly
        ) {}
    }
    
    public record Directories(
        String inbox,
        String logs
    ) {}
    
    public record Scheduler(
        long aggregationInterval
    ) {}
    
    public record Sse(
        long pushInterval
    ) {}
} 