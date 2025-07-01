package de.mika.hhn.eventlogaggregator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mika.hhn.eventlogaggregator.config.ElaProperties;
import de.mika.hhn.eventlogaggregator.model.Metrics;
import de.mika.hhn.eventlogaggregator.service.MetricRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Server-Sent Events streaming of live metrics
 */
@RestController
@RequestMapping("/stream")
@Tag(name = "Streaming", description = "Endpoints for real-time metrics streaming")
public class StreamController {
    
    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
    
    private final MetricRepository metricRepository;
    private final ElaProperties properties;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Store active SSE connections
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    
    public StreamController(MetricRepository metricRepository, ElaProperties properties, ObjectMapper objectMapper) {
        this.metricRepository = metricRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        startMetricsStreaming();
    }
    
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Stream live metrics",
        description = "Server-Sent Events stream that pushes current metrics every 10 seconds",
        responses = {
            @ApiResponse(responseCode = "200", description = "Metrics stream established successfully")
        }
    )
    public SseEmitter streamMetrics() {
        String connectionId = "conn_" + System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // Keep connection open indefinitely
        
        // Store the connection
        activeConnections.put(connectionId, emitter);
        log.info("New SSE connection established: {}", connectionId);
        
        // Handle connection cleanup
        emitter.onCompletion(() -> {
            activeConnections.remove(connectionId);
            log.info("SSE connection completed: {}", connectionId);
        });
        
        emitter.onTimeout(() -> {
            activeConnections.remove(connectionId);
            log.warn("SSE connection timed out: {}", connectionId);
        });
        
        emitter.onError(throwable -> {
            activeConnections.remove(connectionId);
            log.error("SSE connection error for {}: {}", connectionId, throwable.getMessage());
        });
        
        // Send initial metrics immediately
        sendCurrentMetrics(emitter);
        
        return emitter;
    }
    
    @GetMapping("/status")
    @Operation(
        summary = "Get streaming status",
        description = "Get information about active streaming connections"
    )
    public Map<String, Object> getStreamingStatus() {
        return Map.of(
            "activeConnections", activeConnections.size(),
            "pushIntervalMs", properties.sse().pushInterval(),
            "status", "operational"
        );
    }
    
    private void startMetricsStreaming() {
        long pushInterval = properties.sse().pushInterval();
        
        scheduler.scheduleAtFixedRate(() -> {
            if (!activeConnections.isEmpty()) {
                broadcastCurrentMetrics();
            }
        }, pushInterval, pushInterval, TimeUnit.MILLISECONDS);
        
        log.info("Started metrics streaming with {}ms interval", pushInterval);
    }
    
    private void broadcastCurrentMetrics() {
        try {
            // Get current hourly metrics (most relevant for real-time dashboard)
            Optional<Metrics> hourlyMetrics = metricRepository.getMetrics(
                properties.aggregation().windows().hourly()
            );
            
            if (hourlyMetrics.isPresent()) {
                String metricsJson = objectMapper.writeValueAsString(Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "metrics", hourlyMetrics.get(),
                    "type", "metrics_update"
                ));
                
                // Send to all active connections
                activeConnections.entrySet().removeIf(entry -> {
                    try {
                        entry.getValue().send(SseEmitter.event()
                            .name("metrics")
                            .data(metricsJson, MediaType.APPLICATION_JSON));
                        return false; // Keep connection
                    } catch (IOException e) {
                        log.warn("Failed to send metrics to connection {}: {}", entry.getKey(), e.getMessage());
                        return true; // Remove connection
                    }
                });
                
                if (!activeConnections.isEmpty()) {
                    log.debug("Broadcasted metrics to {} active connections", activeConnections.size());
                }
            } else {
                // Send "no data" message
                String noDataJson = objectMapper.writeValueAsString(Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "message", "No metrics data available yet",
                    "type", "no_data"
                ));
                
                activeConnections.entrySet().removeIf(entry -> {
                    try {
                        entry.getValue().send(SseEmitter.event()
                            .name("status")
                            .data(noDataJson, MediaType.APPLICATION_JSON));
                        return false;
                    } catch (IOException e) {
                        log.warn("Failed to send no-data message to connection {}: {}", entry.getKey(), e.getMessage());
                        return true;
                    }
                });
            }
            
        } catch (Exception e) {
            log.error("Error during metrics broadcasting: {}", e.getMessage(), e);
        }
    }
    
    private void sendCurrentMetrics(SseEmitter emitter) {
        try {
            Optional<Metrics> hourlyMetrics = metricRepository.getMetrics(
                properties.aggregation().windows().hourly()
            );
            
            String data;
            if (hourlyMetrics.isPresent()) {
                data = objectMapper.writeValueAsString(Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "metrics", hourlyMetrics.get(),
                    "type", "initial_metrics"
                ));
            } else {
                data = objectMapper.writeValueAsString(Map.of(
                    "timestamp", System.currentTimeMillis(),
                    "message", "No metrics data available yet - waiting for events",
                    "type", "initial_no_data"
                ));
            }
            
            emitter.send(SseEmitter.event()
                .name("initial")
                .data(data, MediaType.APPLICATION_JSON));
                
        } catch (IOException e) {
            log.warn("Failed to send initial metrics: {}", e.getMessage());
        }
    }
} 