package de.mika.hhn.eventlogaggregator.service;

import de.mika.hhn.eventlogaggregator.model.Metrics;

import java.time.Duration;
import java.util.Optional;

/**
 * Repository interface for storing and retrieving aggregated metrics
 */
public interface MetricRepository {
    
    /**
     * Store metrics for a specific time window
     */
    void saveMetrics(Duration window, Metrics metrics);
    
    /**
     * Retrieve metrics for a specific time window
     */
    Optional<Metrics> getMetrics(Duration window);
    
    /**
     * Clear all stored metrics
     */
    void clearAll();
    
    /**
     * Get the latest metrics for a time window
     */
    default Optional<Metrics> getLatestMetrics(Duration window) {
        return getMetrics(window);
    }
} 