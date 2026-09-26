package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

import com.example.notificationservice.model.NotificationEvent;

@Service
public class SmsService {

    public void send(NotificationEvent event) {

        System.out.println();
        System.out.println("📱 ================================");
        System.out.println("📱 SMS SENT");
        System.out.println("📱 ================================");
        System.out.println("To      : " + event.getPhone());
        System.out.println("Message : Your booking was successful.");
        System.out.println("Order   : " + event.getOrderId());
        System.out.println("Bill    : " + event.getBillId());
        System.out.println("Total   : ₹" + event.getTotalAmount());
        System.out.println("================================");
    }
}