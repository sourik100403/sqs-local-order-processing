#!/bin/bash

PROJECT_DIR="$HOME/Desktop/sqs-local-demo"
API_DIR="$PROJECT_DIR/order-api"
WORKER_DIR="$PROJECT_DIR/order-worker"
LOG_DIR="$PROJECT_DIR/logs"

echo "=========================================="
echo " Starting SQS Local Demo"
echo "=========================================="

mkdir -p "$LOG_DIR"

# ------------------------------------------
# 1. Start LocalStack
# ------------------------------------------

echo ""
echo "[1/4] Starting LocalStack..."

cd "$PROJECT_DIR"

docker compose up -d

echo "Waiting for LocalStack..."

sleep 5

# ------------------------------------------
# 2. Create/check SQS queue
# ------------------------------------------

echo ""
echo "[2/4] Checking SQS queue..."

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=ap-south-1

QUEUE_URL="http://localhost:4566/000000000000/order-queue"

aws --endpoint-url=http://localhost:4566 \
    sqs create-queue \
    --queue-name order-queue \
    --region ap-south-1 \
    >/dev/null 2>&1 || true

echo "SQS queue ready:"
echo "$QUEUE_URL"

# ------------------------------------------
# 3. Stop old applications
# ------------------------------------------

echo ""
echo "[3/4] Checking ports..."

for PORT in 8080 8081
do

    PID=$(lsof -ti :"$PORT" 2>/dev/null)

    if [ -n "$PID" ]
    then
        echo "Stopping process on port $PORT..."
        kill $PID 2>/dev/null || true
    fi

done

sleep 2

# ------------------------------------------
# 4. Start applications
# ------------------------------------------

echo ""
echo "[4/4] Starting Order Worker..."

cd "$WORKER_DIR"

nohup mvn spring-boot:run \
    > "$LOG_DIR/order-worker.log" 2>&1 &

WORKER_PID=$!

echo "Order Worker PID: $WORKER_PID"

echo ""
echo "Starting Order API..."

cd "$API_DIR"

nohup mvn spring-boot:run \
    > "$LOG_DIR/order-api.log" 2>&1 &

API_PID=$!

echo "Order API PID: $API_PID"

echo ""
echo "=========================================="
echo " All services started"
echo "=========================================="

echo ""
echo "LocalStack:"
echo "http://localhost:4566"

echo ""
echo "Order API:"
echo "http://localhost:8080"

echo ""
echo "SQS:"
echo "$QUEUE_URL"

echo ""
echo "Worker log:"
echo "tail -f $LOG_DIR/order-worker.log"

echo ""
echo "API log:"
echo "tail -f $LOG_DIR/order-api.log"

echo ""
echo "=========================================="
echo " Test Order"
echo "=========================================="

echo ""
echo 'curl -X POST http://localhost:8080/api/orders \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '\''{"orderId":"ORD-2001","product":"Sony ZV-E10","amount":65000}'\'''

echo ""
echo "=========================================="