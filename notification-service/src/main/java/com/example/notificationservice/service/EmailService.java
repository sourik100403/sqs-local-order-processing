package com.example.notificationservice.service;

import org.springframework.stereotype.Service;

import com.example.notificationservice.model.NotificationEvent;

@Service
public class EmailService {

    public void send(NotificationEvent event) {

        System.out.println();
        System.out.println("📧 ================================");
        System.out.println("📧 EMAIL SENT");
        System.out.println("📧 ================================");
        System.out.println("To       : " + event.getEmail());
        System.out.println("Customer : " + event.getCustomerName());
        System.out.println("Subject  : Booking Successful");
        System.out.println();
        System.out.println("Hello " + event.getCustomerName() + ",");
        System.out.println("Your booking was successful.");
        System.out.println("Order ID : " + event.getOrderId());
        System.out.println("Bill ID  : " + event.getBillId());
        System.out.println("Product  : " + event.getProduct());
        System.out.println("Amount   : ₹" + event.getAmount());
        System.out.println("Tax      : ₹" + event.getTax());
        System.out.println("Total    : ₹" + event.getTotalAmount());
        System.out.println();
        System.out.println("Thank you for your booking.");
        System.out.println("================================");
    }
}