package de.mika.hhn.eventlogaggregator.service;

import de.mika.hhn.eventlogaggregator.model.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of MetricRepository using ConcurrentHashMap
 * for thread-safe storage of aggregated metrics
 */
@Repository
public class InMemoryMetricRepository implements MetricRepository {
    
    private static final Logger log = LoggerFactory.getLogger(InMemoryMetricRepository.class);
    
    private final ConcurrentHashMap<Duration, Metrics> metricsStore = new ConcurrentHashMap<>();
    
    @Override
    public void saveMetrics(Duration window, Metrics metrics) {
        metricsStore.put(window, metrics);
        log.debug("Saved metrics for window {}: activeUsers={}, eventsPerMinute={}", 
            window, metrics.activeUsers(), metrics.eventsPerMinute());
    }
    
    @Override
    public Optional<Metrics> getMetrics(Duration window) {
        Metrics metrics = metricsStore.get(window);
        if (metrics != null) {
            log.debug("Retrieved metrics for window {}: activeUsers={}, eventsPerMinute={}", 
                window, metrics.activeUsers(), metrics.eventsPerMinute());
        } else {
            log.debug("No metrics found for window {}", window);
        }
        return Optional.ofNullable(metrics);
    }
    
    @Override
    public void clearAll() {
        int size = metricsStore.size();
        metricsStore.clear();
        log.info("Cleared {} metric entries from repository", size);
    }
    
    /**
     * Get current size of metrics store (for monitoring)
     */
    public int size() {
        return metricsStore.size();
    }
    
    /**
     * Check if metrics exist for a specific window
     */
    public boolean hasMetrics(Duration window) {
        return metricsStore.containsKey(window);
    }
} 