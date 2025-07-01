package de.mika.hhn.eventlogaggregator.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Event data structure representing user actions")
public record Event(
    @Schema(description = "Type of the event", example = "MESSAGE", allowableValues = {"MESSAGE", "LOGIN", "LOGOUT", "JOIN_CHANNEL", "LEAVE_CHANNEL", "USER_ACTION"})
    String type,
    
    @Schema(description = "Timestamp when the event occurred", example = "2024-01-15T10:30:00.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    Instant timestamp,
    
    @Schema(description = "Unique identifier of the user", example = "user123")
    String userId,
    
    @Schema(description = "Additional event data")
    Map<String, Object> payload
) {} 