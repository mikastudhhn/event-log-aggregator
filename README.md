# Event-Log Aggregator

Ein **Real-Time Chat Analytics System** für die Verarbeitung von Event-Daten.

## Was macht diese App?

Diese Spring Boot Anwendung sammelt Chat-Events (wie Logins, Messages, etc.) und erstellt daraus Statistiken in Echtzeit. Die Daten kommen als JSON-Dateien rein und werden zu übersichtlichen Metriken verarbeitet.

## Hauptfunktionen

- 📁 **Automatische Datei-Verarbeitung** - Lege JSON-Dateien in den `data/inbox/` Ordner
- 🔍 **Event-Validierung** - Prüft ob die JSON-Daten korrekt sind
- 📊 **Statistiken erstellen** - Zählt Events pro Stunde/Tag/Woche
- 🌐 **Web-Dashboard** - Zeigt Live-Statistiken im Browser
- 🔄 **REST API** - Für andere Programme zum Abrufen der Daten

## Schnellstart

```bash
# App starten
mvn spring-boot:run

# Test-Daten verarbeiten (neues Terminal)
cp demo-events/*.json data/inbox/

# Dashboard öffnen
# http://localhost:8080/dashboard.html
```

## Wichtige URLs

- **Live Dashboard:** http://localhost:8080/dashboard.html
- **API Dokumentation:** http://localhost:8080/swagger-ui.html
- **Health Check:** http://localhost:8080/actuator/health

## Wie funktioniert es?

1. **Events empfangen:** JSON-Dateien werden in `data/inbox/` gelegt
2. **Validierung:** Prüft ob die Daten dem Schema entsprechen
3. **Aggregation:** Zählt Events und erstellt Statistiken
4. **Bereitstellung:** Daten per REST API und Live-Dashboard abrufbar

## Beispiel Event

```json
{
  "type": "MESSAGE",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "userId": "user123",
  "payload": {
    "channel": "general",
    "message": "Hello World!"
  }
}
```

## API Endpoints

- `GET /metrics/hourly` - Stündliche Statistiken
- `GET /metrics/daily` - Tägliche Statistiken
- `GET /metrics/top-channels` - Beliebteste Channels
- `POST /events` - Event per HTTP senden
- `GET /stream` - Live-Updates für Dashboard

## Technologien

- **Spring Boot** - Java Web Framework
- **Maven** - Build-Tool
- **JSON Schema** - Daten-Validierung
- **Server-Sent Events** - Live-Updates
- **Spring Actuator** - Health Monitoring

## Projekt-Struktur

```
src/main/java/
├── controller/     # REST API Endpoints
├── service/       # Geschäftslogik (Parsing, Aggregation)
├── model/         # Datenklassen (Event, Metrics)
├── config/        # Konfiguration
└── EventLogAggregatorApplication.java

src/main/resources/
├── application.yml       # App-Konfiguration
├── event-schema.json    # Event-Validierung
└── static/dashboard.html # Web-Dashboard

data/inbox/              # Hier JSON-Dateien reinlegen
demo-events/            # Beispiel-Daten zum Testen
```

## Nächste Schritte 

- Tests schreiben (Unit & Integration Tests)
- Docker Container erstellen
- Database statt In-Memory Speicher
- Erweiterte Dashboard-Features