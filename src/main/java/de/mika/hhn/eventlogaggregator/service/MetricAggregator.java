package de.mika.hhn.eventlogaggregator.service;

import de.mika.hhn.eventlogaggregator.config.ElaProperties;
import de.mika.hhn.eventlogaggregator.model.ChannelCount;
import de.mika.hhn.eventlogaggregator.model.Event;
import de.mika.hhn.eventlogaggregator.model.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Service for aggregating events into rolling-window metrics
 */
@Service
public class MetricAggregator {
    
    private static final Logger log = LoggerFactory.getLogger(MetricAggregator.class);
    
    private final ElaProperties properties;
    private final MetricRepository metricRepository;
    
    // Ring-Buffer für Events pro Zeitfenster
    private final Map<Duration, ConcurrentLinkedQueue<TimestampedEvent>> eventWindows = new ConcurrentHashMap<>();
    
    public MetricAggregator(ElaProperties properties, MetricRepository metricRepository) {
        this.properties = properties;
        this.metricRepository = metricRepository;
        initializeWindows();
    }
    
    private void initializeWindows() {
        var windows = properties.aggregation().windows();
        eventWindows.put(windows.hourly(), new ConcurrentLinkedQueue<>());
        eventWindows.put(windows.daily(), new ConcurrentLinkedQueue<>());
        eventWindows.put(windows.weekly(), new ConcurrentLinkedQueue<>());
        log.info("Initialized metric windows: hourly={}, daily={}, weekly={}", 
            windows.hourly(), windows.daily(), windows.weekly());
    }
    
    /**
     * Add a single event to all time windows
     */
    public void addEvent(Event event) {
        if (event == null) {
            return;
        }
        
        TimestampedEvent timestampedEvent = new TimestampedEvent(event, Instant.now());
        
        // Add to all time windows
        eventWindows.values().forEach(queue -> queue.offer(timestampedEvent));
        
        log.debug("Added event to aggregation: type={}, userId={}", event.type(), event.userId());
    }
    
    /**
     * Add multiple events
     */
    public void addEvents(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        events.forEach(this::addEvent);
        log.debug("Added {} events to aggregation", events.size());
    }
    
    /**
     * Aggregate and update metrics for all time windows
     */
    public void aggregateMetrics() {
        var windows = properties.aggregation().windows();
        
        aggregateAndSave(windows.hourly());
        aggregateAndSave(windows.daily()); 
        aggregateAndSave(windows.weekly());
        
        log.debug("Completed metric aggregation for all windows");
    }
    
    private void aggregateAndSave(Duration window) {
        try {
            // Clean expired events from window
            cleanExpiredEvents(window);
            
            // Calculate metrics
            Metrics metrics = calculateMetrics(window);
            
            // Save to repository
            metricRepository.saveMetrics(window, metrics);
            
        } catch (Exception e) {
            log.error("Error during aggregation for window {}: {}", window, e.getMessage(), e);
        }
    }
    
    private void cleanExpiredEvents(Duration window) {
        ConcurrentLinkedQueue<TimestampedEvent> queue = eventWindows.get(window);
        if (queue == null) return;
        
        Instant cutoff = Instant.now().minus(window);
        int removedCount = 0;
        
        // Remove expired events from the beginning of the queue
        while (!queue.isEmpty() && queue.peek().timestamp().isBefore(cutoff)) {
            queue.poll();
            removedCount++;
        }
        
        if (removedCount > 0) {
            log.debug("Removed {} expired events from {} window", removedCount, window);
        }
    }
    
    private Metrics calculateMetrics(Duration window) {
        ConcurrentLinkedQueue<TimestampedEvent> queue = eventWindows.get(window);
        if (queue == null || queue.isEmpty()) {
            return new Metrics(window, 0, 0, Collections.emptyList());
        }
        
        List<Event> events = queue.stream()
            .map(te -> te.event())
            .collect(Collectors.toList());
        
        // Calculate active users
        long activeUsers = events.stream()
            .map(Event::userId)
            .distinct()
            .count();
        
        // Calculate events per minute
        long eventsPerMinute = calculateEventsPerMinute(events.size(), window);
        
        // Calculate top channels
        List<ChannelCount> topChannels = calculateTopChannels(events, 5);
        
        return new Metrics(window, activeUsers, eventsPerMinute, topChannels);
    }
    
    private long calculateEventsPerMinute(int eventCount, Duration window) {
        long windowMinutes = window.toMinutes();
        if (windowMinutes == 0) {
            return eventCount; // For very short windows
        }
        return eventCount / windowMinutes;
    }
    
    private List<ChannelCount> calculateTopChannels(List<Event> events, int topN) {
        return events.stream()
            .map(Event::payload)
            .filter(payload -> payload.containsKey("channel"))
            .map(payload -> (String) payload.get("channel"))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(
                channel -> channel,
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> new ChannelCount(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Long.compare(b.count(), a.count())) // Descending order
            .limit(topN)
            .toList();
    }
    
    /**
     * Get current event count for a specific window (for monitoring)
     */
    public int getCurrentEventCount(Duration window) {
        ConcurrentLinkedQueue<TimestampedEvent> queue = eventWindows.get(window);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Clear all events from all windows
     */
    public void clearAllEvents() {
        eventWindows.values().forEach(ConcurrentLinkedQueue::clear);
        log.info("Cleared all events from aggregation windows");
    }
    
    /**
     * Internal record to store events with their processing timestamp
     */
    private record TimestampedEvent(Event event, Instant timestamp) {}
} 