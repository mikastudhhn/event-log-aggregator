package de.mika.hhn.eventlogaggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class EventLogAggregatorApplication {
    
    private static final Logger log = LoggerFactory.getLogger(EventLogAggregatorApplication.class);

    public static void main(String[] args) {
        log.info("🚀 Starting Event-Log Aggregator...");
        SpringApplication.run(EventLogAggregatorApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("✅ Event-Log Aggregator is ready!");
        log.info("📊 Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("📈 Metrics API: http://localhost:8080/metrics/hourly");
        log.info("🔄 Live Stream: http://localhost:8080/stream");
        log.info("📥 Event Submission: POST http://localhost:8080/events");
        log.info("🏥 Health Check: http://localhost:8080/actuator/health");
    }
}
