package de.mika.hhn.eventlogaggregator.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Channel statistics with event count")
public record ChannelCount(
    @Schema(description = "Channel name", example = "#lobby")
    String channel,
    
    @Schema(description = "Number of events in this channel", example = "900")
    long count
) {} 