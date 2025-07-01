#!/bin/bash

echo "🧪 Event-Log Aggregator Test Script"
echo "=================================="

# Check if server is running
echo "1. Checking if server is running..."
if curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "✅ Server is running"
else
    echo "❌ Server is not running. Start with: mvn spring-boot:run"
    exit 1
fi

echo ""
echo "2. Testing Event Submission..."

# Test single event submission
echo "   Submitting single event..."
curl -X POST http://localhost:8080/events \
     -H "Content-Type: application/json" \
     -d '{
       "type": "LOGIN",
       "timestamp": "2024-01-15T10:30:00.000Z",
       "userId": "testuser123",
       "payload": {
         "source": "web"
       }
     }'

echo -e "\n"

# Test batch event submission
echo "   Submitting batch events..."
curl -X POST http://localhost:8080/events/batch \
     -H "Content-Type: application/json" \
     -d '[
       {
         "type": "JOIN_CHANNEL",
         "timestamp": "2024-01-15T10:31:00.000Z",
         "userId": "testuser123",
         "payload": {
           "channel": "#lobby"
         }
       },
       {
         "type": "MESSAGE",
         "timestamp": "2024-01-15T10:31:30.000Z",
         "userId": "testuser456",
         "payload": {
           "channel": "#lobby",
           "message": "Hello World!"
         }
       }
     ]'

echo -e "\n\n"
echo "3. Testing File-based Event Processing..."

# Copy demo events to inbox
echo "   Copying demo events to inbox..."
cp demo-events/sample-events.json data/inbox/

echo "   Waiting 3 seconds for processing..."
sleep 3

echo ""
echo "4. Retrieving Metrics..."

echo "   Hourly metrics:"
curl -s http://localhost:8080/metrics/hourly | jq '.' || curl -s http://localhost:8080/metrics/hourly

echo -e "\n   Top channels:"
curl -s http://localhost:8080/metrics/top-channels?n=3 | jq '.' || curl -s http://localhost:8080/metrics/top-channels?n=3

echo -e "\n"
echo "5. Testing API Endpoints..."

echo "   Event status:"
curl -s http://localhost:8080/events/status | jq '.' || curl -s http://localhost:8080/events/status

echo -e "\n   Stream status:"
curl -s http://localhost:8080/stream/status | jq '.' || curl -s http://localhost:8080/stream/status

echo -e "\n   Metrics health:"
curl -s http://localhost:8080/metrics/health

echo -e "\n\n"
echo "🎉 Test completed!"
echo ""
echo "🔗 Useful Links:"
echo "   📊 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "   🔄 Live Stream: http://localhost:8080/stream"
echo "   📈 Metrics: http://localhost:8080/metrics/hourly"
echo "   🏥 Health: http://localhost:8080/actuator/health" 