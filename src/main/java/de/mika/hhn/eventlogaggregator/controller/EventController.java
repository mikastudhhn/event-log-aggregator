package de.mika.hhn.eventlogaggregator.controller;

import de.mika.hhn.eventlogaggregator.model.Event;
import de.mika.hhn.eventlogaggregator.service.EventParser;
import de.mika.hhn.eventlogaggregator.service.MetricAggregator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for receiving events via HTTP POST
 */
@RestController
@RequestMapping("/events")
@Tag(name = "Events", description = "Endpoints for submitting events to the aggregator")
public class EventController {
    
    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    
    private final EventParser eventParser;
    private final MetricAggregator metricAggregator;
    
    public EventController(EventParser eventParser, MetricAggregator metricAggregator) {
        this.eventParser = eventParser;
        this.metricAggregator = metricAggregator;
    }
    
    @PostMapping
    @Operation(
        summary = "Submit single event",
        description = "Submit a single event for processing and aggregation",
        responses = {
            @ApiResponse(responseCode = "200", description = "Event processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid event format or validation failed")
        }
    )
    public ResponseEntity<Map<String, Object>> submitEvent(@RequestBody String eventJson) {
        log.debug("Received single event via HTTP POST");
        
        try {
            Event event = eventParser.parseEvent(eventJson);
            
            if (event != null) {
                metricAggregator.addEvent(event);
                log.info("Successfully processed event: type={}, userId={}", event.type(), event.userId());
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Event processed successfully",
                    "eventType", event.type(),
                    "userId", event.userId()
                ));
            } else {
                log.warn("Failed to parse event: invalid format or validation failed");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid event format or validation failed"
                ));
            }
            
        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error processing event: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/batch")
    @Operation(
        summary = "Submit multiple events",
        description = "Submit an array of events for processing and aggregation",
        responses = {
            @ApiResponse(responseCode = "200", description = "Events processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid events format or validation failed")
        }
    )
    public ResponseEntity<Map<String, Object>> submitEvents(@RequestBody String eventsJson) {
        log.debug("Received batch events via HTTP POST");
        
        try {
            List<Event> events = eventParser.parseEvents(eventsJson);
            
            if (!events.isEmpty()) {
                metricAggregator.addEvents(events);
                log.info("Successfully processed {} events", events.size());
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Events processed successfully",
                    "processedCount", events.size()
                ));
            } else {
                log.warn("No valid events found in batch");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "No valid events found in batch"
                ));
            }
            
        } catch (Exception e) {
            log.error("Error processing events batch: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Error processing events: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping
    @Operation(
        summary = "Get event API information",
        description = "Get information about available event endpoints and how to use them"
    )
    public ResponseEntity<Map<String, Object>> getEventApiInfo() {
        return ResponseEntity.ok(Map.of(
            "message", "Event Log Aggregator - Event Submission API",
            "endpoints", Map.of(
                "POST /events", "Submit a single event",
                "POST /events/batch", "Submit multiple events",
                "GET /events/status", "Get processing status"
            ),
            "sampleEvent", Map.of(
                "type", "MESSAGE",
                "timestamp", "2024-01-15T10:30:00.000Z",
                "userId", "user123",
                "payload", Map.of(
                    "channel", "#lobby",
                    "message", "Hello World!"
                )
            ),
            "documentation", "http://localhost:8080/swagger-ui.html",
            "liveMetrics", "http://localhost:8080/metrics/hourly",
            "liveDashboard", "http://localhost:8080/dashboard.html"
        ));
    }
    
    @GetMapping("/status")
    @Operation(
        summary = "Get event processing status",
        description = "Get current status and statistics of event processing"
    )
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            // This could be extended to include more detailed statistics
            return ResponseEntity.ok(Map.of(
                "status", "operational",
                "message", "Event processing system is running",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("Error getting event processing status: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", "Error getting status: " + e.getMessage()
            ));
        }
    }
} 