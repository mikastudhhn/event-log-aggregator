package de.mika.hhn.eventlogaggregator.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Duration;
import java.util.List;

@Schema(description = "Aggregated metrics for a specific time window")
public record Metrics(
    @Schema(description = "Time window for the metrics", example = "PT1H")
    Duration window,
    
    @Schema(description = "Number of active users in the time window", example = "1280")
    long activeUsers,
    
    @Schema(description = "Average events per minute", example = "534")
    long eventsPerMinute,
    
    @Schema(description = "Top channels by event count")
    List<ChannelCount> topChannels
) {} 