package com.example.orderworker.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.orderworker.entity.Bill;
import com.example.orderworker.entity.Order;
import com.example.orderworker.repository.BillRepository;

@Service
public class BillService {

    private final BillRepository billRepository;

    public BillService(BillRepository billRepository) {
        this.billRepository = billRepository;
    }

    public Bill generateBill(Order order) {

        // Prevent duplicate bill generation
        return billRepository.findByOrderId(order.getOrderId())
                .orElseGet(() -> {

                    double amount = order.getAmount();

                    // Example tax: 18%
                    double tax = amount * 0.18;

                    double total = amount + tax;

                    Bill bill = new Bill();

                    bill.setBillId(
                            "BILL-" + UUID.randomUUID()
                    );

                    bill.setOrderId(
                            order.getOrderId()
                    );

                    bill.setProduct(
                            order.getProduct()
                    );

                    bill.setAmount(amount);

                    bill.setTax(tax);

                    bill.setTotalAmount(total);

                    bill.setStatus("GENERATED");

                    bill.setCreatedAt(
                            LocalDateTime.now()
                    );

                    return billRepository.save(bill);
                });
    }
}