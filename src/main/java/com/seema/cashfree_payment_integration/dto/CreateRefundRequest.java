
package com.seema.cashfree_payment_integration.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateRefundRequest(

        @NotBlank
        String cfPaymentId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal refundAmount

) {}
