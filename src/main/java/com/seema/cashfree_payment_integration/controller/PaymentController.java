package com.seema.cashfree_payment_integration.controller;

import com.seema.cashfree_payment_integration.dto.CreatePaymentRequest;
import com.seema.cashfree_payment_integration.dto.CreatePaymentResponse;
import com.seema.cashfree_payment_integration.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<CreatePaymentResponse> createNewPayment(@Valid @RequestBody CreatePaymentRequest request)
    {
        CreatePaymentResponse createPaymentResponse = paymentService.createOrder(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createPaymentResponse);
    }

}
