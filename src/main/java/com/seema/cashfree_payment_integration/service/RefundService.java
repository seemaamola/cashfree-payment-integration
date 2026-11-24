
package com.seema.cashfree_payment_integration.service;

import com.seema.cashfree_payment_integration.entity.PaymentTransaction;
import com.seema.cashfree_payment_integration.entity.Refund;
import com.seema.cashfree_payment_integration.entity.RefundStatus;
import com.seema.cashfree_payment_integration.repository.PaymentTransactionRepository;
import com.seema.cashfree_payment_integration.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RefundRepository refundRepository;

    @Transactional
    public Refund initiateRefund(String cfPaymentId, BigDecimal refundAmount) {

        // 1. Find and lock the original payment
        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findByCfPaymentIdForUpdate(cfPaymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment transaction not found"
                ));

        // 2. Check whether payment was successful
        if (!"SUCCESS".equals(paymentTransaction.getTransactionStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only successful payments can be refunded"
            );
        }

        // 3. Validate refund amount
        if (refundAmount == null ||
                refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount must be greater than zero"
            );
        }

        BigDecimal originalAmount = paymentTransaction.getPaymentAmount();

        if (originalAmount == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Original payment amount is missing"
            );
        }

        if (refundAmount.compareTo(originalAmount) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount cannot exceed original payment amount"
            );
        }

        // 4. Calculate already reserved refunds
        BigDecimal alreadyRefunded = refundRepository
                .sumRefundAmountByPaymentIdAndStatuses(
                        cfPaymentId,
                        List.of(RefundStatus.PENDING, RefundStatus.SUCCESS)
                );

        // 5. Calculate remaining refundable balance
        BigDecimal remainingRefundable = originalAmount.subtract(alreadyRefunded);

        if (refundAmount.compareTo(remainingRefundable) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refund amount exceeds remaining refundable balance"
            );
        }

        // 6. Create a pending refund record
        Refund refund = new Refund();
        refund.setRefundId(UUID.randomUUID().toString());
        refund.setPaymentTransaction(paymentTransaction);
        refund.setRefundAmount(refundAmount);
        refund.setRefundStatus(RefundStatus.PENDING);
        refund.setRefundReason("Customer refund request");
        refund.setRefundId(UUID.randomUUID().toString().replace("-", ""));

        // 7. Save and return
        return refundRepository.save(refund);
    }
}
