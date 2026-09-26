package com.example.orderworker.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.orderworker.model.NotificationEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class NotificationProducer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.notification-queue-url}")
    private String notificationQueueUrl;

    public NotificationProducer(
            SqsClient sqsClient,
            ObjectMapper objectMapper) {

        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    public String sendNotification(NotificationEvent event) throws Exception {

        String messageBody = objectMapper.writeValueAsString(event);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(notificationQueueUrl)
                .messageBody(messageBody)
                .build();

        String messageId = sqsClient.sendMessage(request).messageId();

        System.out.println("Notification event sent to SQS");
        System.out.println("Notification Message ID: " + messageId);

        return messageId;
    }
}