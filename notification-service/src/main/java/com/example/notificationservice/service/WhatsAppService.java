package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

import com.example.notificationservice.model.NotificationEvent;

@Service
public class WhatsAppService {

    public void send(NotificationEvent event) {

        System.out.println();
        System.out.println("🟢 ================================");
        System.out.println("🟢 WHATSAPP SENT");
        System.out.println("🟢 ================================");
        System.out.println("To      : " + event.getWhatsapp());
        System.out.println("Message : Booking Successful!");
        System.out.println("Order   : " + event.getOrderId());
        System.out.println("Bill    : " + event.getBillId());
        System.out.println("Product : " + event.getProduct());
        System.out.println("Total   : ₹" + event.getTotalAmount());
        System.out.println("================================");
    }
}