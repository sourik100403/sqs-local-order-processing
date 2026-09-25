# SQS Local Order Processing System

A practical **event-driven order processing system** built with **Spring Boot, Amazon SQS, LocalStack, H2 Database, Docker Compose, and k6**.

This project demonstrates asynchronous order processing where an Order API sends orders to an SQS queue, a Worker processes the order, completes payment processing, generates a bill, saves the order and bill into an H2 database, and finally deletes the successfully processed SQS message.

---

# Architecture

```text
                         Client / k6
                              |
                              | HTTP POST
                              v
                    +-------------------+
                    |    Order API      |
                    |   Spring Boot     |
                    |     :8080         |
                    +---------+---------+
                              |
                              | Send Message
                              v
                    +-------------------+
                    |    LocalStack     |
                    |       SQS         |
                    |     :4566         |
                    +---------+---------+
                              |
                              | Poll / Receive
                              v
                    +-------------------+
                    |   Order Worker    |
                    |   Spring Boot     |
                    |      :8081        |
                    +---------+---------+
                              |
                              v
                    +-------------------+
                    |  Order Processing |
                    +---------+---------+
                              |
                       Payment Success
                              |
                              v
                    +-------------------+
                    | Order = COMPLETED |
                    +---------+---------+
                              |
                              v
                    +-------------------+
                    |   Bill Service    |
                    | Generate Bill     |
                    +---------+---------+
                              |
                    +---------+---------+
                    |                   |
                    v                   v
              +-------------+    +-------------+
              |   ORDERS    |    |    BILLS    |
              |     H2      |    |     H2      |
              +-------------+    +-------------+
                              |
                              v
                    Delete SQS Message
```

---

# Project Overview

The system contains two Spring Boot applications.

## 1. Order API

The Order API receives order requests from clients.

```text
Client
  |
  | POST /api/orders
  v
Order API :8080
  |
  | SendMessage
  v
SQS
```

The API returns after successfully sending the order message to SQS.

It does not wait for:

- Payment processing
- Bill generation
- Database completion

This makes the API asynchronous from the business-processing perspective.

---

# 2. Order Worker

The Order Worker continuously polls the SQS queue.

```text
SQS
 |
 v
Order Worker
 |
 +--> Read order
 |
 +--> Save order as PROCESSING
 |
 +--> Process payment
 |
 +--> Update order to COMPLETED
 |
 +--> Generate bill
 |
 +--> Save bill to H2
 |
 +--> Delete SQS message
```

The Worker runs on:

```text
http://localhost:8081
```

---

# Complete Order Flow

When a client creates an order:

```text
1. Client
      |
      v
2. POST /api/orders
      |
      v
3. Order API
      |
      v
4. Send message to SQS
      |
      v
5. SQS order-queue
      |
      v
6. Order Worker receives message
      |
      v
7. Save order as PROCESSING
      |
      v
8. Process payment
      |
      v
9. Payment successful
      |
      v
10. Update order = COMPLETED
      |
      v
11. Generate bill
      |
      v
12. Save bill to H2
      |
      v
13. Delete SQS message
```

---

# Example

Client sends:

```json
{
  "orderId": "ORD-3001",
  "product": "Sony ZV-E10",
  "amount": 65000
}
```

The API sends this order to SQS.

The Worker processes the order.

After successful processing:

```text
Order:
ORD-3001
Product:
Sony ZV-E10
Amount:
65000
Status:
COMPLETED
```

The Bill Service generates a bill.

Example:

```text
Bill ID:
BILL-xxxxxxxx

Order ID:
ORD-3001

Amount:
65000

Tax:
11700

Total:
76700

Status:
GENERATED
```

Both the order and bill are stored in H2.

---

# Technologies

| Technology | Purpose |
|---|---|
| Java 24 | Programming language |
| Spring Boot 3.5.3 | Application framework |
| Spring Data JPA | Database persistence |
| H2 | Local database |
| Amazon SQS | Message queue |
| LocalStack | Local AWS simulation |
| AWS SDK for Java | SQS integration |
| Docker | Container runtime |
| Docker Compose | Local infrastructure |
| Maven | Build tool |
| k6 | Load testing |
| Bash | Automation |

---

# Project Structure

```text
sqs-local-demo/
│
├── README.md
├── .gitignore
├── docker-compose.yml
├── start-all.sh
├── load-test.js
│
├── logs/
│   ├── order-api.log
│   └── order-worker.log
│
├── localstack/
│
├── order-api/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           └── resources/
│
└── order-worker/
    ├── pom.xml
    ├── data/
    │   └── orderdb.mv.db
    └── src/
        └── main/
            ├── java/
            └── resources/
```

> `target/`, logs, local H2 data, and local environment files should be excluded through `.gitignore`.

---

# Order API

The API exposes:

```text
POST /api/orders
```

Example:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId":"ORD-3001",
    "product":"Sony ZV-E10",
    "amount":65000
  }'
```

Example response:

```json
{
  "messageId": "d1269a52-557a-45b3-81f8-9e22bc0e209c",
  "status": "Order sent to SQS"
}
```

The `messageId` is the SQS message identifier.

---

# LocalStack

LocalStack provides a local AWS environment for development.

Currently the project uses LocalStack only for SQS.

```text
LocalStack
    |
    +--- SQS
```

LocalStack runs on:

```text
http://localhost:4566
```

Docker Compose configuration:

```yaml
services:
  localstack:
    image: localstack/localstack:4.8
    container_name: localstack-sqs
    ports:
      - "4566:4566"
    environment:
      - SERVICES=sqs
      - AWS_DEFAULT_REGION=ap-south-1
    volumes:
      - "./localstack:/var/lib/localstack"
      - "/var/run/docker.sock:/var/run/docker.sock"
```

---

# SQS Queue

Queue name:

```text
order-queue
```

Queue URL:

```text
http://localhost:4566/000000000000/order-queue
```

For local development, dummy credentials are used:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-south-1
```

These credentials are for LocalStack only and are not real AWS credentials.

---

# Check SQS

List queues:

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs list-queues \
  --region ap-south-1
```

Check queue depth:

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/order-queue \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-south-1
```

Example:

```json
{
  "Attributes": {
    "ApproximateNumberOfMessages": "0"
  }
}
```

---

# Order Worker

The Worker uses Spring Scheduling to poll SQS.

The Worker:

1. Receives messages
2. Converts JSON into an order object
3. Saves the order
4. Processes payment
5. Updates order status
6. Generates a bill
7. Saves the bill
8. Deletes the SQS message

---

# Payment Processing

The current application contains simulated payment processing.

Example:

```java
Thread.sleep(500);
```

This is intentionally used to simulate a payment-processing delay for performance testing.

In a real application, this would be replaced by a payment service or payment gateway integration.

---

# Bill Generation

After payment succeeds:

```text
Payment Successful
       |
       v
Order = COMPLETED
       |
       v
BillService
       |
       +--> Generate Bill ID
       |
       +--> Calculate Tax
       |
       +--> Calculate Total
       |
       +--> Save Bill
```

The current example uses an 18% tax calculation:

```text
Tax = Amount × 18%
```

For an order worth:

```text
65000
```

the example bill becomes:

```text
Amount = 65000
Tax = 11700
Total = 76700
```

This tax calculation is only a demo and should be replaced by actual business/tax rules in a production system.

---

# Database

The project uses a file-based H2 database.

Configuration:

```properties
spring.datasource.url=jdbc:h2:file:./data/orderdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Database location:

```text
order-worker/data/orderdb.mv.db
```

---

# Database Tables

## ORDERS

The order table stores the order-processing information.

Example:

```text
ID | ORDER_ID  | PRODUCT      | AMOUNT | STATUS
------------------------------------------------
1  | ORD-3001  | Sony ZV-E10  | 65000  | COMPLETED
```

---

## BILLS

The bill table stores generated billing information.

Example:

```text
ID | BILL_ID   | ORDER_ID  | PRODUCT      | AMOUNT | TAX   | TOTAL | STATUS
-----------------------------------------------------------------------------
1  | BILL-xxx  | ORD-3001  | Sony ZV-E10  | 65000  | 11700 | 76700 | GENERATED
```

---

# H2 Console

The project can expose the H2 web console.

Configuration:

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Open:

```text
http://localhost:8081/h2-console
```

Login details:

```text
JDBC URL:
jdbc:h2:file:./data/orderdb

Username:
sa

Password:
leave empty
```

Click **Connect**.

---

# Verify Orders

Run:

```sql
SELECT * FROM ORDERS;
```

---

# Verify Bills

Run:

```sql
SELECT * FROM BILLS;
```

---

# Verify Complete Flow

After sending an order:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-TEST-001","product":"Sony ZV-E10","amount":65000}'
```

Check worker logs:

```bash
grep -nE "Received SQS|Order ID|Payment successful|Order completed|Bill generated|SQS message deleted|Failed to process" \
logs/order-worker.log
```

Expected flow:

```text
Received SQS message
Order ID: ORD-TEST-001
Payment successful
Order completed successfully
Bill generated successfully
SQS message deleted
```

Then verify the database:

```sql
SELECT * FROM ORDERS;

SELECT * FROM BILLS;
```

---

# Start the Entire Application

The project provides:

```text
start-all.sh
```

Make it executable:

```bash
chmod +x start-all.sh
```

Start everything:

```bash
./start-all.sh
```

The script:

```text
1. Starts LocalStack
2. Checks/creates SQS queue
3. Checks application ports
4. Starts Order Worker
5. Starts Order API
```

---

# Service Ports

| Service | Port |
|---|---:|
| Order API | 8080 |
| Order Worker | 8081 |
| LocalStack | 4566 |
| H2 Console | 8081/h2-console |

---

# Logs

Order API:

```bash
tail -f logs/order-api.log
```

Order Worker:

```bash
tail -f logs/order-worker.log
```

---

# Load Testing

The project uses **k6** to test API performance.

Install k6 on macOS:

```bash
brew install k6
```

Verify:

```bash
k6 version
```

Run the load test:

```bash
k6 run load-test.js
```

The load test generates random:

- Order ID
- Product
- Amount

Example:

```json
{
  "orderId": "ORD-123456-AB12",
  "product": "Sony ZV-E10",
  "amount": 67342
}
```

---

# Load Testing Strategy

Load should be increased gradually:

```text
100 requests/sec
       |
       v
1,000 requests/sec
       |
       v
10,000 requests/sec
```

The target experiment is:

```text
10,000 requests/sec
```

for:

```text
30 seconds
```

Approximately:

```text
10,000 × 30
=
300,000 requests
```

The load test measures the behavior of the **local development environment**, not the maximum capacity of AWS SQS.

---

# Performance Bottleneck

The current Worker intentionally contains:

```java
Thread.sleep(500);
```

to simulate payment processing.

With one processing thread, this is approximately:

```text
2 orders/sec
```

while the producer can send significantly more requests.

For example:

```text
Producer
10,000 orders/sec

        ↓

SQS Queue
████████████████████████

        ↓

Worker
~2 orders/sec
```

The queue therefore becomes a buffer between the producer and consumer.

This demonstrates an important distributed-system concept:

> Producer throughput and consumer throughput do not have to be the same.

---

# Monitor Queue Backlog

Run:

```bash
while true; do
  aws --endpoint-url=http://localhost:4566 \
    sqs get-queue-attributes \
    --queue-url http://localhost:4566/000000000000/order-queue \
    --attribute-names ApproximateNumberOfMessages \
    --region ap-south-1

  sleep 1
done
```

---

# Monitor Docker

```bash
docker stats
```

---

# Monitor CPU

```bash
top -o cpu
```

---

# Error Handling

The Worker follows an important rule:

```text
Successful processing
        |
        v
Delete SQS message
```

If processing fails:

```text
Processing failure
        |
        v
Do NOT delete message
```

This allows the message to become available for retry according to SQS visibility-timeout behavior.

---

# Idempotency

The Bill Service checks whether a bill already exists for an order before creating another bill.

Conceptually:

```text
SQS Message
     |
     v
Order ORD-3001
     |
     v
Does bill already exist?
     |
   +---+---+
   |       |
  Yes      No
   |       |
   v       v
Return   Generate
existing  new bill
bill
```

This helps prevent duplicate bill generation when the same SQS message is delivered more than once.

---

# Current Architecture

```text
                    Client
                      |
                      v
                Order API :8080
                      |
                      | SendMessage
                      v
                LocalStack :4566
                      |
                      v
                  SQS Queue
                      |
                      | Poll
                      v
              Order Worker :8081
                      |
              +-------+--------+
              |                |
              v                v
        Order Processing   Payment
              |                |
              +-------+--------+
                      |
                      v
              Order COMPLETED
                      |
                      v
                Bill Service
                      |
              +-------+-------+
              |               |
              v               v
          ORDERS H2       BILLS H2
                      |
                      v
               Delete SQS Message
```

---

# Future Notification System

The next planned feature is a notification system.

After successful order completion and bill generation:

```text
Order Completed
      |
      v
Bill Generated
      |
      v
Notification Event
      |
      +------------+------------+
      |            |            |
      v            v            v
    Email         SMS        Push
```

The notification system can later be separated into its own service and queue.

A future architecture could be:

```text
                 Order API
                     |
                     v
                  SQS
                     |
                     v
               Order Worker
                     |
          +----------+----------+
          |                     |
          v                     v
       Database             Bill Service
                                  |
                                  v
                         Notification Queue
                                  |
                   +--------------+--------------+
                   |              |              |
                   v              v              v
                Email            SMS          Push
```

This keeps notification processing independent from the main order-processing workflow.

---

# Future Production AWS Architecture

The local implementation can later be migrated to AWS.

```text
                         Internet
                            |
                            v
                           ALB
                            |
                +-----------+-----------+
                |                       |
                v                       v
             API #1                  API #2
                |                       |
                +-----------+-----------+
                            |
                            v
                        AWS SQS
                            |
              +-------------+-------------+
              |             |             |
              v             v             v
           Worker #1     Worker #2     Worker #3
              |             |             |
              +-------------+-------------+
                            |
                            v
                           RDS
```

Additional production components can include:

```text
CloudWatch
Auto Scaling
Dead Letter Queue
RDS
ECS / EKS / EC2
ALB
IAM
Secrets Manager
```

---

# Planned Improvements

## 1. Concurrent Workers

Increase the number of messages processed concurrently.

```text
                    SQS
                     |
        +------------+------------+
        |            |            |
        v            v            v
     Worker 1     Worker 2     Worker 3
```

---

## 2. Batch Processing

Receive multiple SQS messages at once.

```text
ReceiveMessage
      |
      +-- Message 1
      +-- Message 2
      +-- Message 3
      ...
      +-- Message 10
```

---

## 3. Batch Delete

Use batch deletion where appropriate instead of deleting every message individually.

---

## 4. Dead Letter Queue

Failed messages can eventually move to a DLQ after a configured number of retries.

```text
SQS
 |
 v
Worker
 |
 +---- Success ---> Delete
 |
 +---- Failure ---> Retry
                       |
                       v
                    Max Retry
                       |
                       v
                      DLQ
```

---

## 5. Monitoring

Add production metrics for:

- API requests/sec
- API latency
- HTTP errors
- SQS queue depth
- Worker throughput
- Processing failures
- Bill generation failures
- DLQ messages
- CPU
- Memory

---

## 6. Notification Service

Add asynchronous:

- Email notifications
- SMS notifications
- Push notifications

without blocking the order-processing workflow.

---

# Key Concepts Demonstrated

This project demonstrates:

- Spring Boot REST API
- Amazon SQS concepts
- LocalStack
- Event-driven architecture
- Asynchronous processing
- Producer/consumer architecture
- Message acknowledgement
- SQS retries
- Idempotency
- Database persistence
- JPA/Hibernate
- H2 database
- Bill generation
- Docker
- Docker Compose
- Bash automation
- k6 load testing
- Queue backlog
- Horizontal scaling concepts
- Distributed-system design

---

# Learning Architecture

The main concept of the project is:

```text
             FAST API
                |
                v
             SQS QUEUE
                |
                v
        ASYNCHRONOUS WORKER
                |
        +-------+-------+
        |               |
        v               v
     ORDER           PAYMENT
        |               |
        +-------+-------+
                |
                v
          ORDER COMPLETED
                |
                v
           BILL SERVICE
                |
                v
          H2 DATABASE
                |
                v
        DELETE SQS MESSAGE
```

The project starts as a local implementation and is designed to evolve toward a production AWS event-driven architecture.

---

# Author

**Sourik Parui**

Learning and building practical projects around:

- AWS
- DevOps
- Spring Boot
- Docker
- CI/CD
- Distributed Systems
- Event-Driven Architecture
- Cloud Infrastructure
- Performance Testing
- System Design