package com.example.orderworker.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import com.example.orderworker.entity.Order;
import com.example.orderworker.model.OrderMessage;
import com.example.orderworker.repository.OrderRepository;

@Service
public class OrderConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public OrderConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            OrderRepository orderRepository
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
    }

    @Scheduled(fixedDelay = 1000)
    public void consumeMessages() {

        ReceiveMessageRequest request =
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(10)
                        .waitTimeSeconds(10)
                        .build();

        List<Message> messages =
                sqsClient.receiveMessage(request)
                        .messages();

        for (Message message : messages) {

            try {

                System.out.println("================================");
                System.out.println("Received SQS message:");
                System.out.println(message.body());
                System.out.println(
                        "Message ID: " + message.messageId()
                );

                // Process order
                processOrder(message);

                // Delete ONLY after successful processing
                deleteMessage(message);

                System.out.println(
                        "Message processed successfully"
                );

            } catch (Exception e) {

                System.err.println(
                        "Failed to process message: "
                                + e.getMessage()
                );

                e.printStackTrace();

                // IMPORTANT:
                // Don't delete the message if processing fails.
                // SQS can make it available again after
                // the visibility timeout.
            }
        }
    }

    private void processOrder(Message message)
            throws Exception {

        System.out.println("Processing order...");

        // ==========================================
        // 1. Convert JSON message → Java object
        // ==========================================

        OrderMessage orderMessage =
                objectMapper.readValue(
                        message.body(),
                        OrderMessage.class
                );

        System.out.println(
                "Order ID: "
                        + orderMessage.getOrderId()
        );

        System.out.println(
                "Product: "
                        + orderMessage.getProduct()
        );

        System.out.println(
                "Amount: "
                        + orderMessage.getAmount()
        );

        // ==========================================
        // 2. Create database entity
        // ==========================================

        Order order = new Order();

        order.setOrderId(
                orderMessage.getOrderId()
        );

        order.setProduct(
                orderMessage.getProduct()
        );

        order.setAmount(
                orderMessage.getAmount()
        );

        order.setStatus("PROCESSING");

        // ==========================================
        // 3. Save order
        // ==========================================

        orderRepository.save(order);

        System.out.println(
                "Order saved to database"
        );

        // ==========================================
        // 4. Process payment
        // ==========================================

        processPayment(order);

        // ==========================================
        // 5. Mark order COMPLETED
        // ==========================================

        order.setStatus("COMPLETED");

        orderRepository.save(order);

        System.out.println(
                "Order completed successfully"
        );
    }

    private void processPayment(Order order) {

        System.out.println(
                "Processing payment for order: "
                        + order.getOrderId()
        );

        try {

            // Simulate payment processing
            Thread.sleep(500);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Payment processing interrupted",
                    e
            );
        }

        System.out.println(
                "Payment successful for order: "
                        + order.getOrderId()
        );
    }

    private void deleteMessage(Message message) {

        DeleteMessageRequest deleteRequest =
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(
                                message.receiptHandle()
                        )
                        .build();

        sqsClient.deleteMessage(deleteRequest);

        System.out.println(
                "SQS message deleted: "
                        + message.messageId()
        );
    }
}
