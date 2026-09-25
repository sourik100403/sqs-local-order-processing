# SQS Local Order Processing Demo

A hands-on **event-driven order processing system** built with **Spring Boot, Amazon SQS, LocalStack, H2 Database, Docker Compose, and k6**.

This project demonstrates how an application can use a message queue to decouple an order-producing API from an asynchronous order-processing worker.

---

## Architecture

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
                              | Receive Message
                              v
                    +-------------------+
                    |   Order Worker    |
                    |   Spring Boot     |
                    |      :8081        |
                    +---------+---------+
                              |
                    +---------+---------+
                    |                   |
                    v                   v
             +-------------+     +-------------+
             |    H2 DB    |     |   Payment   |
             | File-based  |     | Processing  |
             +-------------+     +-------------+
```

---

## Project Overview

The application contains two Spring Boot services:

### 1. Order API

The Order API accepts HTTP requests from clients and sends order information to an SQS queue.

```text
Client
  |
  v
POST /api/orders
  |
  v
Order API
  |
  v
SQS
```

The API does not wait for the complete order processing workflow.

It returns a response after successfully sending the message to SQS.

### 2. Order Worker

The Order Worker continuously polls the SQS queue.

```text
SQS
 |
 v
Order Worker
 |
 +--> Save order
 |
 +--> Process payment
 |
 +--> Update status
 |
 +--> Delete SQS message
```

The worker processes orders asynchronously.

---

# Technologies Used

| Technology | Purpose |
|---|---|
| Java 24 | Programming language |
| Spring Boot 3.5.3 | Application framework |
| Spring Data JPA | Database access |
| H2 Database | Local database |
| Amazon SQS | Message queue |
| LocalStack | Local AWS service simulation |
| AWS SDK for Java | SQS integration |
| Docker | Containerization |
| Docker Compose | Local infrastructure |
| Maven | Build tool |
| k6 | Load testing |
| Bash | Startup automation |

---

# Project Structure

```text
sqs-local-demo/
│
├── docker-compose.yml
├── start-all.sh
├── load-test.js
│
├── logs/
│   ├── order-api.log
│   └── order-worker.log
│
├── order-api/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           └── resources/
│
├── order-worker/
│   ├── pom.xml
│   ├── data/
│   │   └── orderdb.mv.db
│   └── src/
│       └── main/
│           ├── java/
│           └── resources/
│
└── localstack/
```

---

# Architecture Flow

When an order is created:

```text
1. Client sends HTTP request
             |
             v
2. Order API receives request
             |
             v
3. API creates SQS message
             |
             v
4. Message enters order-queue
             |
             v
5. Worker polls SQS
             |
             v
6. Worker reads order
             |
             v
7. Order saved as PROCESSING
             |
             v
8. Payment processing
             |
             v
9. Order updated to COMPLETED
             |
             v
10. SQS message deleted
```

---

# Prerequisites

Install the following:

### Java

```bash
java -version
```

### Maven

```bash
mvn -version
```

### Docker

```bash
docker --version
```

### Docker Compose

```bash
docker compose version
```

### AWS CLI

```bash
aws --version
```

### k6

```bash
brew install k6
```

Verify:

```bash
k6 version
```

---

# Clone the Repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
```

Example:

```bash
git clone https://github.com/YOUR_USERNAME/sqs-local-demo.git
```

Go to the project:

```bash
cd sqs-local-demo
```

---

# Start the Application

The project includes a startup script that starts LocalStack, creates/checks the SQS queue, and starts both Spring Boot applications.

Make the script executable:

```bash
chmod +x start-all.sh
```

Run:

```bash
./start-all.sh
```

The script starts:

```text
LocalStack       → 4566
Order API        → 8080
Order Worker     → 8081
SQS Queue        → order-queue
```

---

# LocalStack Configuration

LocalStack is configured through Docker Compose.

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

LocalStack endpoint:

```text
http://localhost:4566
```

---

# SQS Configuration

The application uses:

```text
Queue Name:
order-queue
```

Queue URL:

```text
http://localhost:4566/000000000000/order-queue
```

For local development, dummy AWS credentials are used:

```bash
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-south-1
```

---

# Check SQS Queue

List queues:

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs list-queues \
  --region ap-south-1
```

Expected:

```text
http://localhost:4566/000000000000/order-queue
```

---

# Create an Order

Use the Order API:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-2001",
    "product": "Sony ZV-E10",
    "amount": 65000
  }'
```

Example response:

```json
{
  "messageId": "ea5755c0-047d-48e6-88e7-25d34c196bcc",
  "status": "Order sent to SQS"
}
```

The important point is that the API returns after sending the message to SQS.

---

# Order Processing

The worker receives the message:

```json
{
  "orderId": "ORD-2001",
  "product": "Sony ZV-E10",
  "amount": 65000
}
```

The worker then:

```text
Receive message
      |
      v
Deserialize JSON
      |
      v
Create Order
      |
      v
Save PROCESSING
      |
      v
Process payment
      |
      v
Update COMPLETED
      |
      v
Delete SQS message
```

---

# Worker Logs

View worker logs:

```bash
tail -f ~/Desktop/sqs-local-demo/logs/order-worker.log
```

Search for order processing:

```bash
grep -nE "Received SQS|Order ID|Order saved|Payment successful|Order completed|SQS message deleted|Failed to process" \
logs/order-worker.log
```

---

# Order API Logs

```bash
tail -f logs/order-api.log
```

---

# H2 Database

The worker uses a file-based H2 database:

```properties
spring.datasource.url=jdbc:h2:file:./data/orderdb
```

Database location:

```text
order-worker/data/orderdb.mv.db
```

Database credentials:

```text
Username: sa
Password: empty
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

Use:

```text
JDBC URL:
jdbc:h2:file:./data/orderdb

Username:
sa

Password:
leave empty
```

Then query:

```sql
SELECT * FROM ORDERS;
```

Example result:

```text
ID | ORDER_ID  | PRODUCT      | AMOUNT | STATUS
-------------------------------------------------
1  | ORD-2001  | Sony ZV-E10  | 65000  | COMPLETED
```

---

# Check SQS Queue Depth

To see how many messages are waiting:

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

A value of `0` means there are approximately no messages currently waiting in the queue.

---

# Load Testing with k6

The project includes a `load-test.js` file.

The load test generates random:

- Order ID
- Product
- Amount

Example generated request:

```json
{
  "orderId": "ORD-1727312345-25-1042-A8K92P",
  "product": "Sony ZV-E10",
  "amount": 67342
}
```

Another request might contain:

```json
{
  "orderId": "ORD-1727312346-31-1043-X7P21M",
  "product": "MacBook Air M3",
  "amount": 92451
}
```

---

# Load Test Progression

It is recommended to increase the load gradually.

```text
100 requests/sec
       ↓
1,000 requests/sec
       ↓
10,000 requests/sec
```

Start with:

```javascript
rate: 100
```

Then:

```javascript
rate: 1000
```

Finally:

```javascript
rate: 10000
```

Run:

```bash
k6 run load-test.js
```

---

# 10,000 Requests/Second Test

The target test is:

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
300,000 HTTP requests
```

This is a local performance experiment and should not be interpreted as the maximum capacity of AWS SQS.

The local system includes:

```text
Mac
Spring Boot
LocalStack
Docker
H2
JVM
```

Any of these can become the bottleneck.

---

# Important Performance Observation

The current worker intentionally contains simulated payment processing:

```java
Thread.sleep(500);
```

Therefore one processing thread can process approximately:

```text
1 / 0.5
=
2 orders/sec
```

while the producer can potentially generate thousands of requests per second.

For example:

```text
Producer:
10,000 orders/sec

Worker:
~2 orders/sec
```

This causes the SQS backlog to increase.

Conceptually:

```text
             10,000/sec
                 |
                 v
          +--------------+
          |     SQS      |
          |    Queue     |
          | ████████████ |
          | ████████████ |
          +------+-------+
                 |
                 | ~2/sec
                 v
              Worker
```

This demonstrates why asynchronous systems often require **horizontal consumer scaling**.

---

# Monitoring During Load Testing

Monitor SQS:

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

Monitor Docker:

```bash
docker stats
```

Monitor processes:

```bash
top -o cpu
```

---

# Current Limitations

This project is currently designed as a **local learning and experimentation environment**.

Current limitations include:

- Single local Order Worker
- H2 instead of production database
- LocalStack instead of AWS SQS
- Simulated payment processing
- No Dead Letter Queue yet
- No production monitoring yet
- No automatic worker scaling yet
- Limited concurrency
- No production-grade distributed tracing
- No complete idempotency implementation yet

---

# Planned Improvements

The next stages of the project are:

## 1. Concurrent SQS Consumers

Increase worker concurrency:

```text
             SQS
              |
       +------+------+------+
       |      |      |      |
       v      v      v      v
    Worker  Worker Worker Worker
       1      2      3      4
```

---

## 2. Batch Processing

Receive multiple messages:

```text
ReceiveMessage
      |
      +-- Message 1
      +-- Message 2
      +-- Message 3
      ...
      +-- Message 10
```

This can improve throughput.

---

## 3. Batch Delete

Instead of deleting messages individually:

```text
DeleteMessage
DeleteMessage
DeleteMessage
...
```

use batch deletion where appropriate.

---

## 4. Idempotency

SQS Standard can deliver duplicate messages.

The application should therefore safely handle:

```text
ORD-2001
ORD-2001
```

without creating duplicate orders.

The `orderId` can be used as an idempotency key.

---

## 5. Dead Letter Queue

Failed messages should eventually be moved to a DLQ:

```text
                SQS
                 |
                 v
             Processing
                 |
          +------+------+
          |             |
       Success        Failure
          |             |
          v             v
       Delete          Retry
                        |
                   Max retries
                        |
                        v
                       DLQ
```

---

## 6. Production AWS Architecture

The local architecture can eventually be migrated to AWS:

```text
                    Internet
                       |
                       v
                      ALB
                       |
             +---------+---------+
             |                   |
             v                   v
         API Instance       API Instance
             |                   |
             +---------+---------+
                       |
                       v
                   AWS SQS
                       |
             +---------+---------+
             |         |         |
             v         v         v
          Worker    Worker    Worker
             |         |         |
             +---------+---------+
                       |
                       v
                      RDS
```

Monitoring can be added with:

```text
CloudWatch
```

for:

- API latency
- API errors
- CPU
- Memory
- SQS queue depth
- Worker throughput
- Failed messages
- DLQ messages

---

# Key Concepts Learned

This project demonstrates:

### REST API

```text
Client → Spring Boot
```

### Message Queue

```text
Producer → SQS → Consumer
```

### Asynchronous Processing

```text
API → SQS
      |
      +---- Worker processes later
```

### Decoupling

The API and worker can scale independently.

### Backpressure

When producers are faster than consumers:

```text
Incoming rate > Processing rate
```

the queue grows.

### Horizontal Scaling

More workers can process more messages concurrently.

### Reliability

Messages can be retried when processing fails.

### Idempotency

Duplicate messages should not create duplicate business operations.

### Load Testing

k6 can generate controlled traffic to measure system behavior.

---

# Useful Commands

### Start everything

```bash
./start-all.sh
```

### Check Docker

```bash
docker ps
```

### Check SQS

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs list-queues \
  --region ap-south-1
```

### Check queue depth

```bash
aws --endpoint-url=http://localhost:4566 \
  sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/order-queue \
  --attribute-names ApproximateNumberOfMessages \
  --region ap-south-1
```

### API logs

```bash
tail -f logs/order-api.log
```

### Worker logs

```bash
tail -f logs/order-worker.log
```

### Docker resource usage

```bash
docker stats
```

### Run load test

```bash
k6 run load-test.js
```

---

# Learning Outcome

This project demonstrates how to build an **asynchronous, event-driven order processing system** locally and provides a foundation for understanding how the same architecture can be deployed using AWS services.

The main architectural principle is:

```text
                FAST PRODUCER
                     |
                     v
                  AWS SQS
                     |
                     v
              SCALABLE CONSUMERS
                     |
                     v
                 DATABASE
```

The local implementation provides a practical foundation for progressing toward a production AWS architecture using:

```text
ALB
 +
Spring Boot
 +
AWS SQS
 +
ECS / EKS / EC2
 +
RDS
 +
CloudWatch
 +
Auto Scaling
 +
DLQ
```

---

## Author

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