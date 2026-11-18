package com.seema.cashfree_payment_integration.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreatePaymentResponse(

        String orderId,
        BigDecimal orderAmount,
        String paymentSessionId,
        String orderStatus

) {
}
