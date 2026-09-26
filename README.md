# SQS Order Processing & Notification System

A practical event-driven order processing system built with **Spring Boot, AWS SQS concepts, LocalStack, H2 Database, Docker, and k6**.

The project demonstrates how an order can be processed asynchronously, a bill can be generated after successful payment, and customer notifications can be triggered through separate Email, SMS, and WhatsApp notification channels.

---

# Architecture

```text
                         ┌─────────────────────┐
                         │       Client        │
                         │   Postman / k6      │
                         └──────────┬──────────┘
                                    │
                                    │ POST /api/orders
                                    ▼
                         ┌─────────────────────┐
                         │     Order API       │
                         │    Spring Boot      │
                         │       :8080         │
                         └──────────┬──────────┘
                                    │
                                    │ Send Order
                                    ▼
                         ┌─────────────────────┐
                         │     order-queue     │
                         │      SQS            │
                         │     LocalStack      │
                         └──────────┬──────────┘
                                    │
                                    │ Receive Order
                                    ▼
                         ┌─────────────────────┐
                         │    Order Worker     │
                         │    Spring Boot      │
                         │       :8081         │
                         └──────────┬──────────┘
                                    │
                    ┌───────────────┴────────────────┐
                    │                                │
                    ▼                                ▼
          ┌──────────────────┐             ┌──────────────────┐
          │   H2 Database    │             │ Payment Process  │
          │                  │             │     SUCCESS      │
          │ ORDERS           │             └────────┬─────────┘
          │ BILLS            │                      │
          └──────────────────┘                      │
                                                    ▼
                                           ┌─────────────────┐
                                           │   BillService   │
                                           │ Generate Bill   │
                                           └────────┬────────┘
                                                    │
                                                    │ ORDER_COMPLETED
                                                    ▼
                                      ┌─────────────────────────┐
                                      │  notification-queue     │
                                      │          SQS            │
                                      │       LocalStack        │
                                      └────────────┬────────────┘
                                                   │
                                                   │ Receive Event
                                                   ▼
                                      ┌─────────────────────────┐
                                      │   Notification Service  │
                                      │      Spring Boot        │
                                      │          :8082           │
                                      └────────────┬────────────┘
                                                   │
                              ┌────────────────────┼────────────────────┐
                              │                    │                    │
                              ▼                    ▼                    ▼
                         ┌─────────┐          ┌─────────┐          ┌───────────┐
                         │  Email  │          │   SMS   │          │ WhatsApp  │
                         │ Service │          │ Service │          │  Service  │
                         └─────────┘          └─────────┘          └───────────┘
```

---

# End-to-End Flow

The complete request flow is:

```text
Customer
   │
   ▼
Order API
   │
   ▼
SQS order-queue
   │
   ▼
Order Worker
   │
   ├── Save Order
   │
   ├── Process Payment
   │
   ├── Mark Order COMPLETED
   │
   ├── Generate Bill
   │
   └── Publish ORDER_COMPLETED event
             │
             ▼
      notification-queue
             │
             ▼
     Notification Service
             │
       ┌─────┼─────┐
       ▼     ▼     ▼
     Email  SMS  WhatsApp
```

---

# Project Structure

```text
sqs-local-demo/
│
├── docker-compose.yml
├── start-all.sh
├── load-test.js
├── README.md
│
├── order-api/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/orderapi/
│           │       ├── controller/
│           │       ├── model/
│           │       └── service/
│           │
│           └── resources/
│
├── order-worker/
│   ├── pom.xml
│   ├── data/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/orderworker/
│           │       ├── config/
│           │       ├── entity/
│           │       ├── model/
│           │       ├── repository/
│           │       └── service/
│           │
│           └── resources/
│
├── notification-service/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/example/notificationservice/
│           │       ├── config/
│           │       ├── model/
│           │       └── service/
│           │
│           └── resources/
│
└── logs/
    ├── order-api.log
    └── order-worker.log
```

---

# Services

## 1. Order API

Port:

```text
8080
```

Responsibility:

- Accept customer orders
- Generate/send order request
- Send order message to SQS
- Return immediately to the client

Example endpoint:

```text
POST http://localhost:8080/api/orders
```

Example request:

```json
{
  "orderId": "ORD-1001",
  "product": "Sony ZV-E10",
  "amount": 65000,
  "customerName": "Sourik",
  "email": "sourik@example.com",
  "phone": "9876543210",
  "whatsapp": "9876543210"
}
```

---

# 2. Order Queue

Queue:

```text
order-queue
```

LocalStack URL:

```text
http://localhost:4566/000000000000/order-queue
```

Purpose:

The API does not directly process the order.

Instead:

```text
API → SQS → Worker
```

This provides asynchronous processing.

---

# 3. Order Worker

Port:

```text
8081
```

The Order Worker consumes messages from `order-queue`.

Responsibilities:

```text
Receive Order
      ↓
Parse Order
      ↓
Save Order
      ↓
Process Payment
      ↓
Payment Success
      ↓
Mark Order COMPLETED
      ↓
Generate Bill
      ↓
Publish Notification Event
```

---

# 4. H2 Database

The worker uses H2 for local development.

Configuration:

```properties
spring.datasource.url=jdbc:h2:file:./data/orderdb
spring.datasource.username=sa
spring.datasource.password=
```

Tables include:

```text
ORDERS
BILLS
```

Order information includes:

```text
orderId
product
amount
customerName
email
phone
whatsapp
status
```

Bill information includes:

```text
billId
orderId
product
amount
tax
totalAmount
status
createdAt
```

---

# 5. Bill Generation

After successful payment:

```text
Payment SUCCESS
      ↓
Order COMPLETED
      ↓
BillService
      ↓
Generate Bill
      ↓
Save Bill to H2
```

The bill contains:

```text
Bill ID
Order ID
Product
Amount
Tax
Total Amount
Status
Created Time
```

The current demo calculates:

```text
Tax = Amount × 18%

Total = Amount + Tax
```

---

# 6. Notification Queue

Queue:

```text
notification-queue
```

LocalStack URL:

```text
http://localhost:4566/000000000000/notification-queue
```

After bill generation, the Order Worker publishes an:

```text
ORDER_COMPLETED
```

event.

Example:

```json
{
  "eventType": "ORDER_COMPLETED",
  "orderId": "ORD-1001",
  "billId": "BILL-123",
  "customerName": "Sourik",
  "email": "sourik@example.com",
  "phone": "9876543210",
  "whatsapp": "9876543210",
  "product": "Sony ZV-E10",
  "amount": 65000,
  "tax": 11700,
  "totalAmount": 76700
}
```

---

# 7. Notification Service

Port:

```text
8082
```

The Notification Service consumes:

```text
notification-queue
```

It receives the completed order and bill information.

Then it sends notifications through:

```text
Email
SMS
WhatsApp
```

Currently these are **local simulated notification services**.

They print the notification to the terminal.

---

# Notification Flow

```text
ORDER_COMPLETED
       │
       ▼
NotificationConsumer
       │
       ├──────────────┐
       │              │
       ▼              ▼
EmailService      SmsService
       │              │
       ▼              ▼
   Email Log       SMS Log

       │
       ▼
WhatsAppService
       │
       ▼
 WhatsApp Log
```

Example:

```text
📧 EMAIL SENT

To       : sourik@example.com
Customer : Sourik
Subject  : Booking Successful

Order ID : ORD-1001
Bill ID  : BILL-123
Product  : Sony ZV-E10
Total    : ₹76700
```

SMS:

```text
📱 SMS SENT

To      : 9876543210
Order   : ORD-1001
Bill    : BILL-123
Total   : ₹76700
```

WhatsApp:

```text
🟢 WHATSAPP SENT

To      : 9876543210
Order   : ORD-1001
Bill    : BILL-123
Product : Sony ZV-E10
Total   : ₹76700
```

---

# LocalStack

LocalStack is used to simulate AWS services locally.

Current service:

```text
SQS
```

LocalStack:

```text
http://localhost:4566
```

Docker image:

```text
localstack/localstack:4.8
```

Region:

```text
ap-south-1
```

---

# Docker Compose

The LocalStack service runs through Docker Compose.

Example:

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

# AWS CLI Configuration for LocalStack

For local development:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-south-1
```

List queues:

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs list-queues \
  --region ap-south-1
```

Expected queues:

```text
order-queue
notification-queue
```

---

# Start the System

From the project root:

```bash
cd ~/Desktop/sqs-local-demo
```

Run:

```bash
./start-all.sh
```

The current startup script starts:

```text
LocalStack
Order Worker
Order API
```

The Notification Service can currently be started separately:

```bash
cd ~/Desktop/sqs-local-demo/notification-service
mvn spring-boot:run
```

---

# Test One Order

Start the services and send:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-TEST-001",
    "product": "Sony ZV-E10",
    "amount": 65000,
    "customerName": "Sourik",
    "email": "sourik@example.com",
    "phone": "9876543210",
    "whatsapp": "9876543210"
  }'
```

Expected API response:

```json
{
  "messageId": "...",
  "status": "Order sent to SQS"
}
```

---

# Verify Order Worker

View logs:

```bash
tail -f ~/Desktop/sqs-local-demo/logs/order-worker.log
```

Expected flow:

```text
Received SQS message
Processing order
Payment successful
Order completed successfully
Bill generated successfully
Notification event sent to SQS
```

---

# Verify Notification Service

Run:

```bash
cd ~/Desktop/sqs-local-demo/notification-service
mvn spring-boot:run
```

Expected:

```text
Notification received

Order ID: ORD-TEST-001
Bill ID: BILL-...
Customer: Sourik
Email: sourik@example.com
Phone: 9876543210
WhatsApp: 9876543210
Total Amount: ₹76700

📧 EMAIL SENT
📱 SMS SENT
🟢 WHATSAPP SENT
```

---

# H2 Console

Open:

```text
http://localhost:8081/h2-console
```

Use:

```text
JDBC URL:
jdbc:h2:file:./data/orderdb

Username:
sa

Password:
```

Password is empty.

Check orders:

```sql
SELECT * FROM ORDERS;
```

Check bills:

```sql
SELECT * FROM BILLS;
```

Count orders:

```sql
SELECT COUNT(*) FROM ORDERS;
```

Count bills:

```sql
SELECT COUNT(*) FROM BILLS;
```

---

# Clean H2 Database

To completely remove the local H2 database:

Stop the worker first.

Then:

```bash
cd ~/Desktop/sqs-local-demo/order-worker
rm -rf data
```

When the worker starts again, H2 creates a fresh database.

---

# Clean SQS Queues

Remove old messages:

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs purge-queue \
  --queue-url http://localhost:4566/000000000000/order-queue \
  --region ap-south-1
```

And:

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs purge-queue \
  --queue-url http://localhost:4566/000000000000/notification-queue \
  --region ap-south-1
```

---

# Random Load Testing

The project uses **k6** to generate random orders.

Run:

```bash
cd ~/Desktop/sqs-local-demo
k6 run load-test.js
```

The test generates random:

```text
Order ID
Product
Amount
Customer Name
Email
Phone
WhatsApp
```

Example:

```json
{
  "orderId": "ORD-1758861234567-12-5-A8F92K",
  "product": "Canon R50",
  "amount": 72342,
  "customerName": "Customer-X7K2P9",
  "email": "customer-a82k91xz@example.com",
  "phone": "9876543210",
  "whatsapp": "9876543210"
}
```

---

# Current k6 Configuration

The initial load test uses:

```text
50 requests/second
30 seconds
```

Therefore:

```text
50 × 30 = 1,500 requests
```

The rate can be increased gradually:

```text
50 req/sec
     ↓
100 req/sec
     ↓
500 req/sec
     ↓
1,000 req/sec
     ↓
5,000 req/sec
     ↓
10,000 req/sec
```

---

# Load Testing Architecture

```text
                    k6
                     │
                     │ 50 req/sec
                     ▼
              ┌──────────────┐
              │   Order API  │
              │    :8080     │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │ order-queue  │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │ Order Worker │
              │    :8081     │
              └──────┬───────┘
                     │
                     ▼
              ┌──────────────┐
              │notification- │
              │    queue     │
              └──────┬───────┘
                     │
                     ▼
          ┌──────────────────────┐
          │ Notification Service │
          │        :8082         │
          └──────────┬───────────┘
                     │
            ┌────────┼────────┐
            ▼        ▼        ▼
          Email     SMS    WhatsApp
```

---

# Why SQS Is Used

Without SQS:

```text
Client
  ↓
Order API
  ↓
Payment
  ↓
Database
  ↓
Email
  ↓
SMS
  ↓
WhatsApp
  ↓
Response
```

The API has to wait for all processing.

With SQS:

```text
Client
  ↓
Order API
  ↓
SQS
  ↓
Immediate Response

       ↓
   Background
   Processing
```

This provides:

- Asynchronous processing
- Decoupling
- Buffering
- Retry capability
- Better fault isolation
- Independent worker scaling

---

# Why Use Two Queues?

We intentionally use two queues.

## Queue 1

```text
order-queue
```

Responsible for:

```text
Order processing
Payment
Database
Bill generation
```

## Queue 2

```text
notification-queue
```

Responsible for:

```text
Email
SMS
WhatsApp
```

This keeps notification processing independent from order processing.

For example, if the SMS provider is unavailable:

```text
Order
  ↓
Payment
  ↓
Bill
  ↓
SUCCESS
```

The order does not need to fail just because SMS is unavailable.

The notification event can remain in the notification pipeline for retry.

---

# Important Production Difference

This project currently uses:

```text
LocalStack
H2
Simulated Email
Simulated SMS
Simulated WhatsApp
```

These are for local learning and testing.

A production AWS architecture could replace them with:

```text
LocalStack
     ↓
AWS SQS

H2
     ↓
Amazon RDS

Simulated Email
     ↓
Amazon SES / Email provider

Simulated SMS
     ↓
Amazon SNS / SMS provider

Simulated WhatsApp
     ↓
WhatsApp Business API / provider
```

The application architecture remains conceptually similar.

---

# Technologies

| Technology | Purpose |
|---|---|
| Java 24 | Application runtime |
| Spring Boot 3.5.3 | Backend services |
| Maven | Build management |
| AWS SQS | Message queues |
| LocalStack | Local AWS simulation |
| H2 | Local database |
| Docker | Local infrastructure |
| Docker Compose | LocalStack orchestration |
| k6 | Load testing |
| Jackson | JSON serialization/deserialization |

---

# Ports

| Service | Port |
|---|---:|
| LocalStack | 4566 |
| Order API | 8080 |
| Order Worker | 8081 |
| Notification Service | 8082 |
| H2 Console | 8081 |

---

# Future Production Improvements

The current project is a learning implementation. Possible production improvements include:

### 1. Real Payment Service

Replace the simulated payment processing with a real payment provider.

### 2. Real Email

Replace:

```text
EmailService
```

with a real email provider.

### 3. Real SMS

Replace:

```text
SmsService
```

with a real SMS provider.

### 4. Real WhatsApp

Replace:

```text
WhatsAppService
```

with the WhatsApp Business API or another provider.

### 5. Database

Replace H2 with:

```text
Amazon RDS PostgreSQL
```

### 6. Dead Letter Queue

Add:

```text
notification-queue
       │
       ├── Success
       │
       └── Failure
              ↓
        Dead Letter Queue
```

### 7. Observability

Add:

```text
CloudWatch
Prometheus
Grafana
OpenTelemetry
```

Monitor:

```text
API latency
SQS queue depth
Worker throughput
Error rate
Notification failures
Database performance
```

### 8. Containerization

Containerize:

```text
Order API
Order Worker
Notification Service
```

### 9. CI/CD

Use:

```text
GitHub
   ↓
GitHub Actions / Jenkins
   ↓
Docker Build
   ↓
Amazon ECR
   ↓
AWS deployment
```

---

# Learning Objectives

This project demonstrates:

- REST API development
- Spring Boot microservices
- AWS SQS concepts
- Event-driven architecture
- Asynchronous processing
- Message producers and consumers
- LocalStack
- H2 database
- JPA/Hibernate
- Bill generation
- Notification pipelines
- Service decoupling
- Random test data generation
- k6 load testing
- Queue monitoring
- Failure/retry concepts
- Microservice architecture
- Production architecture evolution

---

# Final Architecture

```text
                         CLIENT / k6
                              │
                              ▼
                     ┌────────────────┐
                     │   Order API    │
                     │     :8080      │
                     └───────┬────────┘
                             │
                             ▼
                     ┌────────────────┐
                     │  order-queue   │
                     │      SQS       │
                     └───────┬────────┘
                             │
                             ▼
                     ┌────────────────┐
                     │ Order Worker   │
                     │     :8081      │
                     └───────┬────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
                ▼            ▼            ▼
             H2 DB       Payment       Bill
                │         Success     Generation
                │            │            │
                └────────────┴─────┬──────┘
                                   │
                                   ▼
                         ORDER_COMPLETED
                                   │
                                   ▼
                    ┌────────────────────────┐
                    │ notification-queue      │
                    │          SQS             │
                    └────────────┬─────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │  Notification Service   │
                    │         :8082           │
                    └────────────┬─────────────┘
                                 │
                  ┌──────────────┼──────────────┐
                  │              │              │
                  ▼              ▼              ▼
               📧 Email        📱 SMS       🟢 WhatsApp
```

---

# Project Goal

The final goal of this project is to understand how a real-world **event-driven order processing system** can be designed.

A customer creates an order.

The system:

```text
Accepts Order
     ↓
Queues Order
     ↓
Processes Order
     ↓
Processes Payment
     ↓
Generates Bill
     ↓
Publishes Completion Event
     ↓
Queues Notification
     ↓
Sends Email
     ↓
Sends SMS
     ↓
Sends WhatsApp
```

The system is intentionally built in stages so that each component can later be replaced with its production AWS equivalent.