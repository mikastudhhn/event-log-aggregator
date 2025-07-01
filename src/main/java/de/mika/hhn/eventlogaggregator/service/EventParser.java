package de.mika.hhn.eventlogaggregator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import de.mika.hhn.eventlogaggregator.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class EventParser {
    
    private static final Logger log = LoggerFactory.getLogger(EventParser.class);
    private static final String INVALID_LOG_FILE = "logs/invalid.log";
    
    private final ObjectMapper objectMapper;
    private JsonSchema eventSchema;
    
    public EventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @PostConstruct
    public void initSchema() {
        try {
            ClassPathResource schemaResource = new ClassPathResource("event-schema.json");
            try (InputStream schemaStream = schemaResource.getInputStream()) {
                JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
                this.eventSchema = factory.getJsonSchema(
                    objectMapper.readTree(schemaStream)
                );
                log.info("Event schema loaded successfully");
            }
        } catch (Exception e) {
            log.error("Failed to load event schema", e);
            throw new RuntimeException("Could not initialize event schema", e);
        }
    }
    
    /**
     * Parse single event from JSON string
     */
    public Event parseEvent(String jsonString) {
        try {
            // First validate against schema
            if (!validateJson(jsonString)) {
                logInvalidEvent(jsonString, "Schema validation failed");
                return null;
            }
            
            // Then parse to Event object
            Event event = objectMapper.readValue(jsonString, Event.class);
            log.debug("Successfully parsed event: type={}, userId={}", event.type(), event.userId());
            return event;
            
        } catch (Exception e) {
            log.warn("Failed to parse event: {}", e.getMessage());
            logInvalidEvent(jsonString, e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse array of events from JSON string
     */
    public List<Event> parseEvents(String jsonString) {
        try {
            // Try to parse as array first
            List<Event> events = objectMapper.readValue(jsonString, new TypeReference<List<Event>>() {});
            
            // Validate each event individually
            List<Event> validEvents = events.stream()
                .filter(event -> {
                    try {
                        String eventJson = objectMapper.writeValueAsString(event);
                        return validateJson(eventJson);
                    } catch (Exception e) {
                        log.warn("Failed to validate event: {}", e.getMessage());
                        return false;
                    }
                })
                .toList();
            
            log.debug("Successfully parsed {} valid events out of {} total", validEvents.size(), events.size());
            return validEvents;
            
        } catch (Exception e) {
            // If array parsing fails, try single event
            Event singleEvent = parseEvent(jsonString);
            if (singleEvent != null) {
                return List.of(singleEvent);
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * Parse events from file
     */
    public List<Event> parseEventsFromFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            List<Event> events = parseEvents(content);
            log.info("Parsed {} events from file: {}", events.size(), filePath.getFileName());
            return events;
            
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            return Collections.emptyList();
        }
    }
    
    private boolean validateJson(String json) {
        try {
            ProcessingReport report = eventSchema.validate(objectMapper.readTree(json));
            return report.isSuccess();
        } catch (ProcessingException | IOException e) {
            log.warn("JSON validation error: {}", e.getMessage());
            return false;
        }
    }
    
    private void logInvalidEvent(String json, String reason) {
        try {
            String logEntry = String.format("[%s] INVALID EVENT - Reason: %s - JSON: %s%n", 
                LocalDateTime.now(), reason, json);
            
            Path logPath = Path.of(INVALID_LOG_FILE);
            Files.createDirectories(logPath.getParent());
            Files.writeString(logPath, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            log.error("Failed to write to invalid events log", e);
        }
    }
} 