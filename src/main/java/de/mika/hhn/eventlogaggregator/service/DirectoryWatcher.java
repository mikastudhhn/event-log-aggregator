package de.mika.hhn.eventlogaggregator.service;

import de.mika.hhn.eventlogaggregator.config.ElaProperties;
import de.mika.hhn.eventlogaggregator.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for watching the inbox directory for new event files
 */
@Service
public class DirectoryWatcher {
    
    private static final Logger log = LoggerFactory.getLogger(DirectoryWatcher.class);
    
    private final ElaProperties properties;
    private final EventParser eventParser;
    private final MetricAggregator metricAggregator;
    
    private WatchService watchService;
    private ExecutorService executorService;
    private volatile boolean running = false;
    
    public DirectoryWatcher(ElaProperties properties, EventParser eventParser, MetricAggregator metricAggregator) {
        this.properties = properties;
        this.eventParser = eventParser;
        this.metricAggregator = metricAggregator;
    }
    
    @PostConstruct
    public void startWatching() {
        try {
            // Create inbox directory if it doesn't exist
            Path inboxPath = Path.of(properties.directories().inbox());
            Files.createDirectories(inboxPath);
            
            // Initialize WatchService
            watchService = FileSystems.getDefault().newWatchService();
            inboxPath.register(watchService, 
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY);
            
            // Start watching in background thread
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "DirectoryWatcher");
                t.setDaemon(true);
                return t;
            });
            
            executorService.submit(this::watchDirectory);
            running = true;
            
            log.info("Started directory watching on: {}", inboxPath.toAbsolutePath());
            
        } catch (IOException e) {
            log.error("Failed to start directory watcher: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void stopWatching() {
        running = false;
        
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.error("Error closing watch service: {}", e.getMessage());
            }
        }
        
        if (executorService != null) {
            executorService.shutdown();
        }
        
        log.info("Stopped directory watching");
    }
    
    private void watchDirectory() {
        log.info("Directory watcher thread started");
        
        while (running) {
            try {
                WatchKey key = watchService.take(); // Blocking call
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();
                    Path fullPath = Path.of(properties.directories().inbox()).resolve(fileName);
                    
                    if (isJsonFile(fileName)) {
                        log.info("Detected {} event for file: {}", kind.name(), fileName);
                        processEventFile(fullPath);
                    }
                }
                
                // Reset the key
                boolean valid = key.reset();
                if (!valid) {
                    log.warn("Watch key no longer valid, stopping watcher");
                    break;
                }
                
            } catch (InterruptedException e) {
                log.info("Directory watcher interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in directory watcher: {}", e.getMessage(), e);
            }
        }
        
        log.info("Directory watcher thread stopped");
    }
    
    private void processEventFile(Path filePath) {
        try {
            // Small delay to ensure file is fully written
            Thread.sleep(100);
            
            if (!Files.exists(filePath)) {
                log.warn("File no longer exists: {}", filePath);
                return;
            }
            
            log.info("Processing event file: {}", filePath.getFileName());
            
            // Parse events from file
            List<Event> events = eventParser.parseEventsFromFile(filePath);
            
            if (!events.isEmpty()) {
                // Add events to aggregator
                metricAggregator.addEvents(events);
                log.info("Successfully processed {} events from file: {}", events.size(), filePath.getFileName());
                
                // Optionally move processed file to processed directory
                moveProcessedFile(filePath);
            } else {
                log.warn("No valid events found in file: {}", filePath.getFileName());
                moveInvalidFile(filePath);
            }
            
        } catch (Exception e) {
            log.error("Error processing event file {}: {}", filePath.getFileName(), e.getMessage(), e);
            moveInvalidFile(filePath);
        }
    }
    
    private void moveProcessedFile(Path filePath) {
        try {
            Path processedDir = Path.of(properties.directories().inbox(), "processed");
            Files.createDirectories(processedDir);
            
            Path targetPath = processedDir.resolve(filePath.getFileName());
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("Moved processed file to: {}", targetPath);
            
        } catch (IOException e) {
            log.warn("Failed to move processed file {}: {}", filePath.getFileName(), e.getMessage());
            // If move fails, try to delete the file
            try {
                Files.deleteIfExists(filePath);
                log.debug("Deleted processed file: {}", filePath.getFileName());
            } catch (IOException deleteError) {
                log.error("Failed to delete processed file {}: {}", filePath.getFileName(), deleteError.getMessage());
            }
        }
    }
    
    private void moveInvalidFile(Path filePath) {
        try {
            Path invalidDir = Path.of(properties.directories().inbox(), "invalid");
            Files.createDirectories(invalidDir);
            
            Path targetPath = invalidDir.resolve(filePath.getFileName());
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.debug("Moved invalid file to: {}", targetPath);
            
        } catch (IOException e) {
            log.error("Failed to move invalid file {}: {}", filePath.getFileName(), e.getMessage());
        }
    }
    
    private boolean isJsonFile(Path fileName) {
        String name = fileName.toString().toLowerCase();
        return name.endsWith(".json");
    }
    
    /**
     * Get current watcher status
     */
    public boolean isRunning() {
        return running;
    }
} 