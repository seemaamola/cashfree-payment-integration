
package com.seema.cashfree_payment_integration.service;

import com.seema.cashfree_payment_integration.entity.Refund;
import com.seema.cashfree_payment_integration.entity.RefundStatus;
import com.seema.cashfree_payment_integration.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundStatusService {

    private final RefundRepository refundRepository;

    @Transactional
    public Refund updateFromCashfree(
            String refundId,
            String cashfreeStatus
    ) {
        Refund refund = refundRepository.findByRefundId(refundId)
                .orElseThrow(() ->
                        new RuntimeException("Refund not found"));

        if ("SUCCESS".equalsIgnoreCase(cashfreeStatus)) {
            refund.setRefundStatus(RefundStatus.SUCCESS);
        } else if ("FAILED".equalsIgnoreCase(cashfreeStatus)) {
            refund.setRefundStatus(RefundStatus.FAILED);
        } else if ("ONHOLD".equalsIgnoreCase(cashfreeStatus)) {
            refund.setRefundStatus(RefundStatus.ONHOLD);
        } else if ("CANCELLED".equalsIgnoreCase(cashfreeStatus)) {
            refund.setRefundStatus(RefundStatus.CANCELLED);
        } else {
            refund.setRefundStatus(RefundStatus.PENDING);
        }

        return refundRepository.save(refund);
    }
}
