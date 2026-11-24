
package com.seema.cashfree_payment_integration.service;

import com.cashfree.pg.model.RefundEntity;
import com.seema.cashfree_payment_integration.client.CashfreeClient;
import com.seema.cashfree_payment_integration.entity.Refund;
import com.seema.cashfree_payment_integration.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundOrchestrationService {

    private final RefundService refundService;
    private final CashfreeClient cashfreeClient;
    private final RefundStatusService refundStatusService;
    private final RefundRepository refundRepository;

    public Refund processRefund(
            String cfPaymentId,
            java.math.BigDecimal refundAmount
    ) {

        // 1. Reserve refund amount in our database
        Refund refund = refundService.initiateRefund(
                cfPaymentId,
                refundAmount
        );

        // 2. Call Cashfree outside the reservation transaction
        try {
            String orderId = refund.getPaymentTransaction()
                    .getPaymentOrder()
                    .getOrderId();

            RefundEntity response = cashfreeClient.createRefund(
                    orderId,
                    refund.getRefundId(),
                    refund.getRefundAmount(),
                    refund.getRefundReason()
            );

            // 3. Update our local refund status
            return refundStatusService.updateFromCashfree(
                    refund.getRefundId(),
                    String.valueOf(response.getRefundStatus())
            );

        } catch (Exception exception) {
            // Keep PENDING for reconciliation.
            // A timeout does not prove Cashfree rejected the refund.
            throw exception;
        }
    }


    public Refund refreshRefundStatus(String refundId) {

        Refund refund = refundRepository
                .findByRefundIdWithPaymentDetails(refundId)
                .orElseThrow(() ->
                        new RuntimeException("Refund not found"));

        String orderId = refund.getPaymentTransaction()
                .getPaymentOrder()
                .getOrderId();

        RefundEntity response = cashfreeClient.fetchRefund(
                orderId,
                refund.getRefundId()
        );

        return refundStatusService.updateFromCashfree(
                refundId,
                String.valueOf(response.getRefundStatus()));

    }

}
