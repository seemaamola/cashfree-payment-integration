package com.seema.cashfree_payment_integration.service;


import com.cashfree.pg.Cashfree;
import com.seema.cashfree_payment_integration.dto.CreatePaymentRequest;
import com.seema.cashfree_payment_integration.dto.CreatePaymentResponse;
import com.seema.cashfree_payment_integration.entity.PaymentOrder;
import com.seema.cashfree_payment_integration.repository.PaymentOrderRepository;
import com.seema.cashfree_payment_integration.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.cashfree.pg.model.CreateOrderRequest;
import com.cashfree.pg.model.CustomerDetails;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final Cashfree cashfree;


    public PaymentOrder saveOrder(PaymentOrder paymentOrder)
    {
        return paymentOrderRepository.save(paymentOrder);
    }

    public CreatePaymentResponse createOrder(CreatePaymentRequest request)
    {

        PaymentOrder paymentOrder= PaymentOrder.builder()
                .orderId(UUID.randomUUID().toString())
                .orderAmount(request.orderAmount())
                .orderCurrency(request.orderCurrency())
                .customerId(request.customerId())
                .customerPhone(request.customerPhone())
                .customerEmail(request.customerEmail())
                .orderStatus("CREATED")
                .build();

        PaymentOrder savedPaymentOrder = paymentOrderRepository.save(paymentOrder);

        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setCustomerId(savedPaymentOrder.getCustomerId());
        customerDetails.setCustomerPhone(savedPaymentOrder.getCustomerPhone());
        customerDetails.setCustomerEmail(savedPaymentOrder.getCustomerEmail());

        CreateOrderRequest cashfreeRequest = new CreateOrderRequest();

        CreatePaymentResponse response = CreatePaymentResponse.builder()
                .orderId(savedPaymentOrder.getOrderId())
                .orderAmount(savedPaymentOrder.getOrderAmount())
                .orderStatus(savedPaymentOrder.getOrderStatus())
                .paymentSessionId(null)
                .build();

        return response;

    }


}
