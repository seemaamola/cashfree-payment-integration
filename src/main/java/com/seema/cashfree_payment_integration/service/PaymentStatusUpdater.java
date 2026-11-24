package com.seema.cashfree_payment_integration.service;

import com.seema.cashfree_payment_integration.entity.PaymentOrder;
import com.seema.cashfree_payment_integration.entity.PaymentTransaction;
import com.seema.cashfree_payment_integration.repository.PaymentOrderRepository;
import com.seema.cashfree_payment_integration.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentStatusUpdater {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public void markPaymentSuccessful(
            String orderId,
            String cfPaymentId,
            String paymentMethod)
    {
        PaymentOrder paymentOrder = paymentOrderRepository
                .findByOrderIdForUpdate(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Payment order not found"));

        PaymentTransaction paymentTransaction = paymentTransactionRepository
                .findByCfPaymentIdForUpdate(cfPaymentId)
                .orElseThrow(() -> new RuntimeException("Payment transaction not found"));
        if (!paymentTransaction.getPaymentOrder().getOrderId().equals(orderId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment transaction does not belong to this order"
            );
        }
        if ("SUCCESS".equals(paymentTransaction.getTransactionStatus())) {
            return;
        }

        if("PAID".equals(paymentOrder.getOrderStatus()))
        {
            paymentOrder.setOrderStatus("PAID");
            paymentOrderRepository.save(paymentOrder);
        }else {
            // Flag duplicate successful payment for reconciliation.
        }


        //paymentTransaction.setCfPaymentId(cfPaymentId);
        paymentTransaction.setTransactionStatus("SUCCESS");
        paymentTransaction.setPaymentMethod(paymentMethod);

        paymentTransactionRepository.save(paymentTransaction);

        paymentOrder.setOrderStatus("PAID");
        paymentOrderRepository.save(paymentOrder);
    }
}
