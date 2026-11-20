package com.seema.cashfree_payment_integration.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;

    @Column(nullable = false,precision = 12,scale = 2)
    private BigDecimal orderAmount;

    @Column(nullable = false, length = 3)
    private String orderCurrency;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, length = 15)
    private String customerPhone;
    private String customerEmail;
    private String orderStatus;

    @Column(name = "idempotency_key", unique = true,updatable = false)
    private UUID idempotencyKey;
}
