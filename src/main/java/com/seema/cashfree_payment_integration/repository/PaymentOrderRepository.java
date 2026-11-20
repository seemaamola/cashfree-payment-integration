package com.seema.cashfree_payment_integration.repository;

import com.seema.cashfree_payment_integration.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder,Long> {

    Optional<PaymentOrder> findByOrderId(String orderId);

    Optional<PaymentOrder> findByIdempotencyKey(UUID idempotencyKey);
}
