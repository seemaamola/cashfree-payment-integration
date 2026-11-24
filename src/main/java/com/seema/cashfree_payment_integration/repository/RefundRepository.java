package com.seema.cashfree_payment_integration.repository;

import com.seema.cashfree_payment_integration.entity.Refund;
import com.seema.cashfree_payment_integration.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,Long> {

    Optional<Refund> findByRefundId(String refundId);

    List<Refund> findByPaymentTransaction_CfPaymentId(String cfPaymentId);

    @Query("""
       SELECT COALESCE(SUM(r.refundAmount), 0)
       FROM Refund r
       WHERE r.paymentTransaction.cfPaymentId = :cfPaymentId
       AND r.refundStatus IN :statuses
       """)
    BigDecimal sumRefundAmountByPaymentIdAndStatuses(
            @Param("cfPaymentId") String cfPaymentId,
            @Param("statuses") List<RefundStatus> statuses);


    @Query("""
    SELECT r FROM Refund r
    JOIN FETCH r.paymentTransaction pt
    JOIN FETCH pt.paymentOrder
    WHERE r.refundId = :refundId
    """)
    Optional<Refund> findByRefundIdWithPaymentDetails(
            @Param("refundId") String refundId
    );



}
