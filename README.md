# Notification Message Publisher

A Spring Boot application that produces and consumes Avro-encoded messages via Google Cloud Pub/Sub using Apache Camel YAML DSL. This project supports four message types: Order Status, UCC, Tour Appointment Confirmation, and Truckload Confirmation.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Project Structure](#project-structure)
- [Setup Instructions](#setup-instructions)
- [Running the Application](#running-the-application)
- [Avro Serialization/Deserialization](#avro-serializationdeserialization)
- [API Endpoints](#api-endpoints)
- [Batch Publishing](#batch-publishing)
- [Troubleshooting](#troubleshooting)

## Features

- **Producer Service**: Reads JSON files, converts them to Avro format, and publishes to Pub/Sub topics
- **Consumer Service**: Consumes messages from Pub/Sub, deserializes Avro bytes, and stores message counts
- **Avro Schema Support**: Automatic code generation from Avro schema definitions
- **Camel YAML DSL**: All routes defined in YAML for easy configuration
- **Pub/Sub Emulator**: Local testing support with emulator setup
- **REST API**: Endpoints for batch publishing and message count retrieval

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Google Cloud SDK (gcloud) - for Pub/Sub emulator and real GCP setup
- Docker (optional, for running Pub/Sub emulator in a container)

## Project Structure

```
cursor-mock-message-publisher/
├── pom.xml                                    # Maven configuration
├── src/main/
│   ├── java/com/cogarc/notification/
│   │   ├── NotificationApplication.java      # Main Spring Boot application
│   │   ├── producer/
│   │   │   ├── ProducerService.java         # Service for batch publishing
│   │   │   └── ProducerController.java      # REST controller for producer
│   │   ├── consumer/
│   │   │   ├── ConsumerController.java      # REST controller for consumer
│   │   │   ├── MessageStorage.java          # In-memory message storage
│   │   │   ├── MessageCountResponse.java   # Response DTO
│   │   │   └── MessageProcessor.java        # Camel processors for each message type
│   │   └── config/
│   │       └── PubSubConfig.java            # Pub/Sub configuration
│   └── resources/
│       ├── application.yml                   # Application configuration
│       ├── producer-routes.yaml              # Camel routes for producers
│       ├── consumer-routes.yaml              # Camel routes for consumers
│       ├── avro/                             # Avro schema definitions
│       │   ├── OrderStatus.avsc
│       │   ├── UCC.avsc
│       │   ├── TourAppointmentConfirmation.avsc
│       │   └── TruckloadConfirmation.avsc
│       └── samples/                          # Sample JSON messages
│           ├── order-status/
│           ├── ucc/
│           ├── tour-appointment/
│           └── truckload/
├── scripts/
│   ├── setup-pubsub.sh                       # Script to create topics/subscriptions
│   └── batch-publish.sh                      # Script for batch publishing
└── README.md
```

## Setup Instructions

### 1. Clone and Build the Project

```bash
cd cursor-mock-message-publisher
mvn clean install
```

This will:
- Download all dependencies
- Generate Java classes from Avro schemas
- Compile the project

### 2. Set Up Pub/Sub Emulator (for Local Testing)

#### Option A: Using gcloud (Recommended)

1. Install Google Cloud SDK if not already installed:
   ```bash
   # macOS
   brew install google-cloud-sdk
   
   # Or download from: https://cloud.google.com/sdk/docs/install
   ```

2. Start the Pub/Sub emulator:
   ```bash
   gcloud beta emulators pubsub start --project=cogarc-notification-project
   ```

   The emulator will start on port 8085 by default.

3. In a new terminal, set the emulator host:
   ```bash
   export PUBSUB_EMULATOR_HOST=localhost:8085
   ```

#### Option B: Using Docker

```bash
docker run --rm -ti -p 8085:8085 \
  gcr.io/google.com/cloudsdktool/cloud-sdk:emulators \
  gcloud beta emulators pubsub start --project=cogarc-notification-project --host-port=0.0.0.0:8085
```

### 3. Create Topics and Subscriptions

Run the setup script to create all required topics and subscriptions:

```bash
# Make script executable
chmod +x scripts/setup-pubsub.sh

# Run setup (with emulator)
export PUBSUB_EMULATOR_HOST=localhost:8085
./scripts/setup-pubsub.sh

# Or for real GCP (after authenticating with gcloud)
gcloud auth login
./scripts/setup-pubsub.sh
```

The script creates:
- **Topics**: `order-status-topic`, `ucc-topic`, `tour-appointment-topic`, `truckload-topic`
- **Subscriptions**: `order-status-subscription`, `ucc-subscription`, `tour-appointment-subscription`, `truckload-subscription`

## Running the Application

### 1. Start the Application

```bash
# With emulator
export PUBSUB_EMULATOR_HOST=localhost:8085
mvn spring-boot:run

# Or run the JAR
java -jar target/notification-publisher-1.0.0.jar
```

The application will start on `http://localhost:8080`.

### 2. Verify Health

```bash
curl http://localhost:8080/actuator/health
```

## Avro Serialization/Deserialization

### How It Works

1. **Producer Flow**:
   - JSON files are read from configured directories
   - JSON is parsed and converted to Avro `GenericRecord` using the appropriate schema
   - The record is serialized to Avro binary format (bytes)
   - Avro bytes are published to Pub/Sub topics

2. **Consumer Flow**:
   - Avro bytes are consumed from Pub/Sub subscriptions
   - Bytes are deserialized back to Avro `GenericRecord` using the schema
   - The record is converted to the generated Java class (e.g., `OrderStatus`)
   - Message identifier is extracted and stored

### Avro Schema Location

Schemas are defined in `src/main/resources/avro/`:
- `OrderStatus.avsc`
- `UCC.avsc`
- `TourAppointmentConfirmation.avsc`
- `TruckloadConfirmation.avsc`

### Generated Classes

After running `mvn compile`, Avro classes are generated in:
```
target/generated-sources/avro/com/cogarc/notification/avro/
```

These classes are automatically available in your code.

## API Endpoints

### Producer Endpoints

#### Batch Publish Messages
```bash
POST /api/producer/batch/{messageType}
```

**Parameters:**
- `messageType`: One of `order-status`, `ucc`, `tour-appointment`, `truckload`

**Example:**
```bash
curl -X POST http://localhost:8080/api/producer/batch/order-status
```

**Response:**
```json
{
  "messageType": "order-status",
  "publishedCount": 3,
  "status": "success"
}
```

### Consumer Endpoints

#### Get Message Counts
```bash
GET /api/messages/count
```

**Example:**
```bash
curl http://localhost:8080/api/messages/count
```

**Response:**
```json
{
  "order-status": {
    "messageType": "order-status",
    "count": 5,
    "identifiers": ["order-status-001", "order-status-002", ...]
  },
  "ucc": {
    "messageType": "ucc",
    "count": 3,
    "identifiers": ["ucc-001", "ucc-002", ...]
  },
  ...
}
```

## Batch Publishing

### Using the Script

The `batch-publish.sh` script provides an easy way to publish all JSON files of a specific type:

```bash
# Make script executable
chmod +x scripts/batch-publish.sh

# Publish order status messages
./scripts/batch-publish.sh order-status

# Publish UCC messages
./scripts/batch-publish.sh ucc

# Publish tour appointment messages
./scripts/batch-publish.sh tour-appointment

# Publish truckload messages
./scripts/batch-publish.sh truckload
```

### Using REST API Directly

```bash
curl -X POST http://localhost:8080/api/producer/batch/order-status
```

### Sample Message Directories

Place your JSON sample files in:
- `src/main/resources/samples/order-status/`
- `src/main/resources/samples/ucc/`
- `src/main/resources/samples/tour-appointment/`
- `src/main/resources/samples/truckload/`

Each JSON file must match the corresponding Avro schema structure.

## Configuration

### application.yml

Key configuration options:

```yaml
spring:
  cloud:
    gcp:
      project-id: cogarc-notification-project
      pubsub:
        emulator-host: ${PUBSUB_EMULATOR_HOST:localhost:8085}

app:
  pubsub:
    topics:
      order-status: order-status-topic
      ucc: ucc-topic
      tour-appointment: tour-appointment-topic
      truckload: truckload-topic
  samples:
    directories:
      order-status: src/main/resources/samples/order-status
      ucc: src/main/resources/samples/ucc
      tour-appointment: src/main/resources/samples/tour-appointment
      truckload: src/main/resources/samples/truckload
```

## Troubleshooting

### Pub/Sub Emulator Not Running

**Error**: `io.grpc.StatusRuntimeException: UNAVAILABLE`

**Solution**: 
1. Start the emulator: `gcloud beta emulators pubsub start --project=cogarc-notification-project`
2. Set environment variable: `export PUBSUB_EMULATOR_HOST=localhost:8085`
3. Restart the application

### Topics/Subscriptions Not Found

**Error**: `NOT_FOUND: Resource not found`

**Solution**: Run the setup script:
```bash
export PUBSUB_EMULATOR_HOST=localhost:8085
./scripts/setup-pubsub.sh
```

### Avro Schema Errors

**Error**: Schema compilation failures

**Solution**:
1. Ensure all Avro schema files are valid JSON
2. Run `mvn clean compile` to regenerate classes
3. Check that schema namespace matches package structure

### No Messages Consumed

**Possible Causes**:
1. Consumer routes not started (check Camel logs)
2. Subscriptions not created
3. Messages not published to correct topics

**Solution**:
1. Check application logs for Camel route status
2. Verify subscriptions exist: `gcloud pubsub subscriptions list --emulator-host=localhost:8085`
3. Verify messages were published: Check producer logs

### Port Already in Use

**Error**: `Port 8080 is already in use`

**Solution**: Change port in `application.yml`:
```yaml
server:
  port: 8081
```

## Development

### Adding New Message Types

1. Create Avro schema in `src/main/resources/avro/`
2. Add topic/subscription configuration in `application.yml`
3. Add sample directory configuration
4. Create Camel routes in `producer-routes.yaml` and `consumer-routes.yaml`
5. Add processor in `MessageProcessor.java`
6. Update `ProducerService.java` to handle new type
7. Run `mvn clean compile` to generate classes

### Building for Production

```bash
mvn clean package -DskipTests
```

This creates `target/notification-publisher-1.0.0.jar`

## License

This project is for internal use at Cogarc.

## Support

For issues or questions, please contact the development team.

