package de.mika.hhn.eventlogaggregator.service;

import de.mika.hhn.eventlogaggregator.config.ElaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduling automatic metric aggregation
 */
@Service
public class AggregatorScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(AggregatorScheduler.class);
    
    private final MetricAggregator metricAggregator;
    private final ElaProperties properties;
    
    public AggregatorScheduler(MetricAggregator metricAggregator, ElaProperties properties) {
        this.metricAggregator = metricAggregator;
        this.properties = properties;
    }
    
    /**
     * Scheduled task to aggregate metrics
     * Runs every second (1000ms) as configured in application.yml
     */
    @Scheduled(fixedRate = 1000) // 1 second interval
    public void aggregateMetrics() {
        try {
            metricAggregator.aggregateMetrics();
            log.debug("Scheduled metric aggregation completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled metric aggregation: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Scheduled task to log system health every minute
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void logSystemHealth() {
        try {
            var windows = properties.aggregation().windows();
            int hourlyEvents = metricAggregator.getCurrentEventCount(windows.hourly());
            int dailyEvents = metricAggregator.getCurrentEventCount(windows.daily());
            int weeklyEvents = metricAggregator.getCurrentEventCount(windows.weekly());
            
            log.info("System Health - Events in windows: hourly={}, daily={}, weekly={}", 
                hourlyEvents, dailyEvents, weeklyEvents);
                
        } catch (Exception e) {
            log.error("Error during system health check: {}", e.getMessage(), e);
        }
    }
} 