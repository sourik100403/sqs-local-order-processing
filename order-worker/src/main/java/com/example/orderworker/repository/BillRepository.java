package com.example.orderworker.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.orderworker.entity.Bill;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByOrderId(String orderId);
}