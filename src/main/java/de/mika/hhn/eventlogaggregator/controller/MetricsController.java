package de.mika.hhn.eventlogaggregator.controller;

import de.mika.hhn.eventlogaggregator.config.ElaProperties;
import de.mika.hhn.eventlogaggregator.model.ChannelCount;
import de.mika.hhn.eventlogaggregator.model.Metrics;
import de.mika.hhn.eventlogaggregator.service.MetricRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for retrieving aggregated metrics
 */
@RestController
@RequestMapping("/metrics")
@Tag(name = "Metrics", description = "Endpoints for retrieving aggregated event metrics")
public class MetricsController {
    
    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);
    
    private final MetricRepository metricRepository;
    private final ElaProperties properties;
    
    public MetricsController(MetricRepository metricRepository, ElaProperties properties) {
        this.metricRepository = metricRepository;
        this.properties = properties;
    }
    
    @GetMapping("/hourly")
    @Operation(
        summary = "Get hourly metrics",
        description = "Retrieve aggregated metrics for the last hour",
        responses = {
            @ApiResponse(responseCode = "200", description = "Hourly metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No metrics available for the hourly window")
        }
    )
    public ResponseEntity<Metrics> getHourlyMetrics() {
        log.debug("Retrieving hourly metrics");
        
        Optional<Metrics> metrics = metricRepository.getMetrics(
            properties.aggregation().windows().hourly()
        );
        
        if (metrics.isPresent()) {
            log.info("Retrieved hourly metrics: activeUsers={}, eventsPerMinute={}", 
                metrics.get().activeUsers(), metrics.get().eventsPerMinute());
            return ResponseEntity.ok(metrics.get());
        } else {
            log.warn("No hourly metrics available");
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/daily")
    @Operation(
        summary = "Get daily metrics",
        description = "Retrieve aggregated metrics for the last 24 hours",
        responses = {
            @ApiResponse(responseCode = "200", description = "Daily metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No metrics available for the daily window")
        }
    )
    public ResponseEntity<Metrics> getDailyMetrics() {
        log.debug("Retrieving daily metrics");
        
        Optional<Metrics> metrics = metricRepository.getMetrics(
            properties.aggregation().windows().daily()
        );
        
        if (metrics.isPresent()) {
            log.info("Retrieved daily metrics: activeUsers={}, eventsPerMinute={}", 
                metrics.get().activeUsers(), metrics.get().eventsPerMinute());
            return ResponseEntity.ok(metrics.get());
        } else {
            log.warn("No daily metrics available");
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/weekly")
    @Operation(
        summary = "Get weekly metrics",
        description = "Retrieve aggregated metrics for the last week",
        responses = {
            @ApiResponse(responseCode = "200", description = "Weekly metrics retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No metrics available for the weekly window")
        }
    )
    public ResponseEntity<Metrics> getWeeklyMetrics() {
        log.debug("Retrieving weekly metrics");
        
        Optional<Metrics> metrics = metricRepository.getMetrics(
            properties.aggregation().windows().weekly()
        );
        
        if (metrics.isPresent()) {
            log.info("Retrieved weekly metrics: activeUsers={}, eventsPerMinute={}", 
                metrics.get().activeUsers(), metrics.get().eventsPerMinute());
            return ResponseEntity.ok(metrics.get());
        } else {
            log.warn("No weekly metrics available");
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/top-channels")
    @Operation(
        summary = "Get top channels by activity",
        description = "Retrieve the most active channels from hourly metrics",
        responses = {
            @ApiResponse(responseCode = "200", description = "Top channels retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No channel data available")
        }
    )
    public ResponseEntity<List<ChannelCount>> getTopChannels(
        @Parameter(description = "Number of top channels to return", example = "5")
        @RequestParam(defaultValue = "5") int n
    ) {
        log.debug("Retrieving top {} channels", n);
        
        Optional<Metrics> hourlyMetrics = metricRepository.getMetrics(
            properties.aggregation().windows().hourly()
        );
        
        if (hourlyMetrics.isPresent()) {
            List<ChannelCount> topChannels = hourlyMetrics.get().topChannels().stream()
                .limit(n)
                .toList();
            
            log.info("Retrieved {} top channels", topChannels.size());
            return ResponseEntity.ok(topChannels);
        } else {
            log.warn("No channel data available");
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Health check for metrics system",
        description = "Check if metrics are being collected and stored properly"
    )
    public ResponseEntity<String> getHealthStatus() {
        boolean hasHourlyMetrics = metricRepository.getMetrics(
            properties.aggregation().windows().hourly()
        ).isPresent();
        
        if (hasHourlyMetrics) {
            return ResponseEntity.ok("Metrics system is operational");
        } else {
            return ResponseEntity.ok("Metrics system is running but no data collected yet");
        }
    }
} 