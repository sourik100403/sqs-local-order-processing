package com.example.orderapi.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.orderapi.model.OrderRequest;
import com.example.orderapi.service.OrderProducer;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer orderProducer;

    public OrderController(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody OrderRequest request) throws Exception {

        String messageId = orderProducer.sendOrder(request);

        return ResponseEntity.ok(
                Map.of(
                        "messageId", messageId,
                        "status", "Order sent to SQS"
                )
        );
    }
}