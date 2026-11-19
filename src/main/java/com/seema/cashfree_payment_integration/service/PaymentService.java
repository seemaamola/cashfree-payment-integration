package com.seema.cashfree_payment_integration.service;

import com.cashfree.pg.Cashfree;
import com.cashfree.pg.model.PaymentEntity;
import com.seema.cashfree_payment_integration.dto.CreatePaymentRequest;
import com.seema.cashfree_payment_integration.dto.CreatePaymentResponse;
import com.seema.cashfree_payment_integration.entity.PaymentOrder;
import com.seema.cashfree_payment_integration.entity.PaymentTransaction;
import com.seema.cashfree_payment_integration.repository.PaymentOrderRepository;
import com.seema.cashfree_payment_integration.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.cashfree.pg.model.CreateOrderRequest;
import com.cashfree.pg.model.CustomerDetails;
import com.cashfree.pg.ApiResponse;
import com.cashfree.pg.model.OrderEntity;
import com.cashfree.pg.ApiException;
import com.cashfree.pg.model.PaymentEntity.PaymentStatusEnum;

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

    @Transactional
    public CreatePaymentResponse createOrder(CreatePaymentRequest request)
    {
        //Creating a payment order on our system
        PaymentOrder paymentOrder= PaymentOrder.builder()
                .orderId(UUID.randomUUID().toString())
                .orderAmount(request.orderAmount())
                .orderCurrency(request.orderCurrency())
                .customerId(request.customerId())
                .customerPhone(request.customerPhone())
                .customerEmail(request.customerEmail())
                .orderStatus("CREATED")
                .build();
        //saving it
        PaymentOrder savedPaymentOrder = paymentOrderRepository.save(paymentOrder);

        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setCustomerId(savedPaymentOrder.getCustomerId());
        customerDetails.setCustomerPhone(savedPaymentOrder.getCustomerPhone());
        customerDetails.setCustomerEmail(savedPaymentOrder.getCustomerEmail());

        //creating a cashfree CreateOrderRequest request
        CreateOrderRequest cashfreeRequest = new CreateOrderRequest();
        cashfreeRequest.setOrderId(savedPaymentOrder.getOrderId());
        cashfreeRequest.setOrderAmount(savedPaymentOrder.getOrderAmount());
        cashfreeRequest.setOrderCurrency(savedPaymentOrder.getOrderCurrency());
        cashfreeRequest.setCustomerDetails(customerDetails);

        try {
            ApiResponse<OrderEntity> cashfreeResponse =
                    cashfree.PGCreateOrder(
                            cashfreeRequest,
                            null,
                            null,
                            null
                    );

            OrderEntity cashfreeOrder = cashfreeResponse.getData();
            //setting payment session id returnd from cashfree
            String paymentSessionId = cashfreeOrder.getPaymentSessionId();


            PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                    .transactionId(UUID.randomUUID().toString())
                    .paymentOrder(savedPaymentOrder)
                    .paymentSessionId(paymentSessionId)
                    .transactionStatus("PENDING")
                    .paymentMethod(null)
                    .build();

            paymentTransactionRepository.save(paymentTransaction);

            savedPaymentOrder.setOrderStatus("PAYMENT_SESSION_CREATED");
            paymentOrderRepository.save(savedPaymentOrder);


            return CreatePaymentResponse.builder()
                    .orderId(savedPaymentOrder.getOrderId())
                    .orderAmount(savedPaymentOrder.getOrderAmount())
                    .orderStatus(savedPaymentOrder.getOrderStatus())
                    .paymentSessionId(paymentSessionId)
                    .build();



        } catch (ApiException e) {
            throw new RuntimeException("Cashfree order creation failed", e);
        }



    }

    public List<PaymentEntity> fetchPaymentsFromCashfree(String orderId)
    {
        try {
            ApiResponse<List<PaymentEntity>> paymentsResponse =
                    cashfree.PGOrderFetchPayments(
                            orderId,
                            null,
                            null,
                            null
                    );

            return paymentsResponse.getData();
        }
        catch (ApiException e)
        {
            throw new RuntimeException("Unable to fetch payment details from Cashfree", e);
        }
    }

    public void verifyPayment(String orderId) {

        //Retrieve the local PaymentOrder
        PaymentOrder paymentOrder = paymentOrderRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Payment order not found")
                );

        //Fetch payment attempts from Cashfree.
        List<PaymentEntity> payments =
                fetchPaymentsFromCashfree(orderId);

        //Find a successful payment.
        Optional<PaymentEntity> successfulPaymentOptional = payments.stream()
                .filter(payment ->
                        payment.getPaymentStatus()==PaymentStatusEnum.SUCCESS)
                .findFirst();
        if(successfulPaymentOptional.isEmpty())
        {
            return;
        }
        PaymentEntity successfulPayment = successfulPaymentOptional.get();

        //verify order amount
        if(!orderId.equals(successfulPayment.getOrderId()))
        {
            throw new RuntimeException("Payment order ID mismatch");
        }
        //verify amount
        if(successfulPayment.getPaymentAmount()==null ||
                successfulPayment.getPaymentAmount()
                        .compareTo(paymentOrder.getOrderAmount())!=0){

            throw new RuntimeException("Payment amount mismatch");
        }
        //verify currency
        if(successfulPayment.getPaymentCurrency()==null||!paymentOrder.getOrderCurrency()
                .equalsIgnoreCase(successfulPayment.getPaymentCurrency())){
            throw new RuntimeException("Payment currency mismatch");
        }
        // Verify Cashfree payment ID exists
        if(successfulPayment.getCfPaymentId()==null || successfulPayment.getCfPaymentId().isBlank())
        {
            throw new RuntimeException("Cashfree payment ID is missing");
        }

        PaymentTransaction paymentTransaction =
                paymentTransactionRepository
                        .findFirstByPaymentOrder_OrderIdOrderByIdDesc(orderId)
                        .orElseThrow(()-> new RuntimeException("Payment transaction not found"));

        paymentTransaction.setCfPaymentId(
                successfulPayment.getCfPaymentId());

        paymentTransaction.setTransactionStatus("SUCCESS");

    }

}
