package com.example.orderapi.service;

import com.example.orderapi.model.OrderRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
public class OrderProducer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public OrderProducer(
            SqsClient sqsClient,
            ObjectMapper objectMapper
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    public String sendOrder(OrderRequest order)
            throws JsonProcessingException {

        String message =
                objectMapper.writeValueAsString(order);

        SendMessageRequest request =
                SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(message)
                        .build();

        SendMessageResponse response =
                sqsClient.sendMessage(request);

        return response.messageId();
    }
}