package com.example.notificationservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.notificationservice.model.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Service
public class NotificationConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;
private final SmsService smsService;
private final WhatsAppService whatsAppService;

    @Value("${aws.sqs.notification-queue-url}")
    private String queueUrl;

   public NotificationConsumer(
        SqsClient sqsClient,
        ObjectMapper objectMapper,
        EmailService emailService,
        SmsService smsService,
        WhatsAppService whatsAppService) {

    this.sqsClient = sqsClient;
    this.objectMapper = objectMapper;
    this.emailService = emailService;
    this.smsService = smsService;
    this.whatsAppService = whatsAppService;
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
                sqsClient.receiveMessage(request).messages();

        for (Message message : messages) {

            try {

                System.out.println("================================");
                System.out.println("Notification received");
                System.out.println("SQS Message ID: "
                        + message.messageId());

                System.out.println("Message:");
                System.out.println(message.body());

                NotificationEvent event =
                        objectMapper.readValue(
                                message.body(),
                                NotificationEvent.class);

                processNotification(event);

                deleteMessage(message);

                System.out.println(
                        "Notification processed successfully");

                System.out.println("================================");

            } catch (Exception e) {

                System.err.println(
                        "Notification processing failed: "
                                + e.getMessage());

                e.printStackTrace();

                // Do NOT delete the message.
                // SQS can deliver it again.
            }
        }
    }

    private void processNotification(
            NotificationEvent event) {

        System.out.println("Order ID: "
                + event.getOrderId());

        System.out.println("Bill ID: "
                + event.getBillId());

        System.out.println("Customer: "
                + event.getCustomerName());

        System.out.println("Email: "
                + event.getEmail());

        System.out.println("Phone: "
                + event.getPhone());

        System.out.println("WhatsApp: "
                + event.getWhatsapp());

        System.out.println("Total Amount: ₹"
                + event.getTotalAmount());

        System.out.println(
                "Ready to send notifications...");
                emailService.send(event);

smsService.send(event);

whatsAppService.send(event);
    }

    private void deleteMessage(Message message) {

        DeleteMessageRequest deleteRequest =
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(
                                message.receiptHandle())
                        .build();

        sqsClient.deleteMessage(deleteRequest);

        System.out.println(
                "SQS notification message deleted");
    }
}