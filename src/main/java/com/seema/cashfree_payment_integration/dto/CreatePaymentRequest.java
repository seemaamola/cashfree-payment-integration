package com.seema.cashfree_payment_integration.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "order amount is required")
        @DecimalMin(value = "1.00", message = "order amount must be at least 1")
        BigDecimal orderAmount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3,max=3, message = "currency must be a 3 letter code")
        @Pattern(regexp = "^[A-Z]{3}$",
                message = "Currency must be a valid 3-letter uppercase code")
        String orderCurrency,

        @NotBlank(message = "Customer ID is required")
        @Size(max = 50, message = "Customer ID cannot exceed 50 characters")
        String customerId,

        @NotBlank(message = "Customer phone is required")
        @Size(min = 8, max = 15, message = "Invalid phone number length")
        @Pattern(regexp = "^\\+?[0-9]{8,15}$",
                message = "Invalid phone number")
        String customerPhone,

        @Email(message = "Please provide a valid email")
        @NotBlank
        String customerEmail
) {
}
