# CRUD Application Architecture

## Overview
This application implements a comprehensive CRUD operations system using **Spring Boot**, **Apache Kafka**, **Apache Camel**, and **Vert.x Verticles**. The architecture follows the flow: **Client Input → Kafka Topic → Camel Bridge → Verticles (1 or 2)** based on operation conditions.

## Architecture Flow

```
Client Request
      ↓
REST Controller (UserController)
      ↓
Unified Kafka Producer
      ↓
Kafka Topic (crud-operations-topic)
      ↓
Apache Camel Bridge (CrudOperationRouter)
      ↓
    Routing Decision
   ↙              ↘
Verticle 1         Verticle 2
(CREATE/UPDATE)    (READ/DELETE/GET_ALL)
      ↓              ↓
User Service       User Service
      ↓              ↓
H2 Database        H2 Database
```

## Components

### 1. REST Controller (`UserController`)
- **Location**: `com.example.crud.controller.UserController`
- **Purpose**: Handles HTTP requests and routes them to appropriate services
- **New Endpoints**:
  - `POST /api/users/kafka/create` - Create user via Kafka flow
  - `PUT /api/users/kafka/update` - Update user via Kafka flow
  - `GET /api/users/kafka/read/{id}` - Read user via Kafka flow
  - `DELETE /api/users/kafka/delete/{id}` - Delete user via Kafka flow
  - `GET /api/users/kafka/all` - Get all users via Kafka flow

### 2. Unified Kafka Producer (`UnifiedKafkaProducer`)
- **Location**: `com.example.crud.kafka.UnifiedKafkaProducer`
- **Purpose**: Sends all CRUD operations to a single Kafka topic with operation metadata
- **Topic**: `crud-operations-topic`
- **Message Structure**:
  ```json
  {
    "operation": "CREATE|UPDATE|READ|DELETE|GET_ALL",
    "data": "User object or ID"
  }
  ```

### 3. Apache Camel Bridge (`CrudOperationRouter`)
- **Location**: `com.example.crud.camel.CrudOperationRouter`
- **Purpose**: Consumes from Kafka and routes messages to appropriate Verticles
- **Routing Logic**:
  - `CREATE`, `UPDATE` → Verticle 1
  - `READ`, `DELETE`, `GET_ALL` → Verticle 2

### 4. Verticle 1 (`CrudVerticle1`)
- **Location**: `com.example.crud.vertx.CrudVerticle1`
- **Purpose**: Handles CREATE and UPDATE operations
- **EventBus Channel**: `verticle1.operations`
- **Operations**:
  - CREATE: Creates new user in database
  - UPDATE: Updates existing user in database

### 5. Verticle 2 (`CrudVerticle2`)
- **Location**: `com.example.crud.vertx.CrudVerticle2`
- **Purpose**: Handles READ, DELETE, and GET_ALL operations
- **EventBus Channel**: `verticle2.operations`
- **Operations**:
  - READ: Retrieves user by ID
  - DELETE: Deletes user by ID
  - GET_ALL: Retrieves all users

## Technology Stack

- **Spring Boot 3.5.0**: Main application framework
- **Apache Kafka**: Message broker for asynchronous communication
- **Apache Camel 4.2.0**: Integration framework for routing and mediation
- **Vert.x 4.5.1**: Reactive toolkit for event-driven applications
- **H2 Database**: In-memory database for development
- **Maven**: Build and dependency management

## Dependencies Added

```xml
<!-- Apache Camel Spring Boot Starter -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-spring-boot-starter</artifactId>
    <version>4.2.0</version>
</dependency>

<!-- Apache Camel Kafka Component -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-kafka-starter</artifactId>
    <version>4.2.0</version>
</dependency>

<!-- Apache Camel Direct Component -->
<dependency>
    <groupId>org.apache.camel.springboot</groupId>
    <artifactId>camel-direct-starter</artifactId>
    <version>4.2.0</version>
</dependency>
```

## Configuration

### Kafka Configuration (`application.yml`)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: crud-app-group
      auto-offset-reset: earliest

camel:
  component:
    kafka:
      brokers: localhost:9092
```

## Running the Application

### Prerequisites
1. **Java 17+**
2. **Apache Kafka** running on `localhost:9092`
3. **Maven 3.6+**

### Steps
1. Start Kafka:
   ```bash
   # Start Zookeeper
   bin/zookeeper-server-start.sh config/zookeeper.properties
   
   # Start Kafka
   bin/kafka-server-start.sh config/server.properties
   ```

2. Run the application:
   ```bash
   cd /Users/akshaypandey/CRUD/mosip
   mvn spring-boot:run
   ```

3. Access the application:
   - **REST API**: `http://localhost:8080/api/users`
   - **H2 Console**: `http://localhost:8080/h2-console`

## Testing the Flow

### Example: Create User via Kafka Flow
```bash
curl -X POST http://localhost:8080/api/users/kafka/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com"
  }'
```

### Example: Read User via Kafka Flow
```bash
curl -X GET http://localhost:8080/api/users/kafka/read/1
```

## Monitoring and Logs

The application provides detailed logging at each stage:
- **Controller**: Request received
- **Kafka Producer**: Message sent to topic
- **Camel Bridge**: Message routing decisions
- **Verticles**: Operation processing
- **Service Layer**: Database operations

## Backward Compatibility

The application maintains backward compatibility with:
- Original REST endpoints (`/api/users`)
- Original Vert.x EventBus operations
- Original Kafka consumers

## Benefits of This Architecture

1. **Scalability**: Asynchronous processing via Kafka
2. **Modularity**: Clear separation of concerns
3. **Flexibility**: Easy to add new operation types
4. **Monitoring**: Comprehensive logging at each stage
5. **Resilience**: Message-driven architecture with retry capabilities
6. **Performance**: Non-blocking operations with Vert.x
