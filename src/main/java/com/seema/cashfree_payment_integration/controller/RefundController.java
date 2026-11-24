
package com.seema.cashfree_payment_integration.controller;

import com.seema.cashfree_payment_integration.dto.CreateRefundRequest;
import com.seema.cashfree_payment_integration.entity.Refund;
import com.seema.cashfree_payment_integration.service.RefundOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundOrchestrationService refundOrchestrationService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRefund(
            @Valid @RequestBody CreateRefundRequest request
    ) {

        Refund refund = refundOrchestrationService.processRefund(
                request.cfPaymentId(),
                request.refundAmount()
        );

        Map<String, Object> response = Map.of(
                "refundId", refund.getRefundId(),
                "refundAmount", refund.getRefundAmount(),
                "refundStatus", refund.getRefundStatus().name(),
                "message", "Refund request processed"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
