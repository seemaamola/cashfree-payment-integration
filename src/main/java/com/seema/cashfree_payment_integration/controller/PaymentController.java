package com.seema.cashfree_payment_integration.controller;

import com.seema.cashfree_payment_integration.dto.CreatePaymentRequest;
import com.seema.cashfree_payment_integration.dto.CreatePaymentResponse;
import com.seema.cashfree_payment_integration.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createNewPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey)
    {
        CreatePaymentResponse response = paymentService.createOrder(request,idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{orderId}/verify")
    public ResponseEntity<Void> verifyPayment(@PathVariable String orderId)
    {
        paymentService.verifyPayment(orderId);

        return ResponseEntity.noContent().build();
    }

}
