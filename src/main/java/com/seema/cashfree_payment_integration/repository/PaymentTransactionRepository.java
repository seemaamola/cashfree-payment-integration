package com.seema.cashfree_payment_integration.repository;

import com.seema.cashfree_payment_integration.entity.PaymentTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction,Long> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    Optional<PaymentTransaction> findFirstByPaymentOrder_OrderIdOrderByIdDesc(String orderId);

    Optional<PaymentTransaction> findByCfPaymentId(String cfPaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.cfPaymentId = :cfPaymentId")
    Optional<PaymentTransaction> findByCfPaymentIdForUpdate(
            @Param("cfPaymentId") String cfPaymentId
    );
}
