package com.seema.cashfree_payment_integration.service;

import com.seema.cashfree_payment_integration.dto.CreatePaymentRequest;
import com.seema.cashfree_payment_integration.dto.CreatePaymentResponse;
import com.seema.cashfree_payment_integration.entity.PaymentOrder;
import com.seema.cashfree_payment_integration.entity.PaymentTransaction;
import com.seema.cashfree_payment_integration.repository.PaymentOrderRepository;
import com.seema.cashfree_payment_integration.repository.PaymentTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cashfree.pg.model.CreateOrderRequest;
import com.cashfree.pg.model.CustomerDetails;
import com.cashfree.pg.ApiResponse;
import com.cashfree.pg.Cashfree;
import com.cashfree.pg.model.PaymentEntity;
import com.cashfree.pg.model.OrderEntity;
import com.cashfree.pg.ApiException;
import com.cashfree.pg.model.PaymentEntity.PaymentStatusEnum;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final Cashfree cashfree;
    private final PaymentStatusUpdater paymentStatusUpdater;


    public PaymentOrder saveOrder(PaymentOrder paymentOrder)
    {
        return paymentOrderRepository.save(paymentOrder);
    }

    private CreatePaymentResponse buildExistingPaymentResponse(
            PaymentOrder existingOrder,
            CreatePaymentRequest request) {

        //compare Existing order with request order
        boolean sameRequest =
                        existingOrder.getOrderAmount().compareTo(request.orderAmount()) == 0
                        && existingOrder.getOrderCurrency().equalsIgnoreCase(request.orderCurrency())
                        && Objects.equals(existingOrder.getCustomerId(), request.customerId())
                        && Objects.equals(existingOrder.getCustomerPhone(), request.customerPhone())
                        && Objects.equals(existingOrder.getCustomerEmail(), request.customerEmail());

        //If they are same then return 409 CONFLICT
        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Idempotency key was already used with different payment details.");
        }

        //Retrieve the existing transaction
        //paymentSessionId is stored in PaymentTransaction, not directly in PaymentOrder.
        PaymentTransaction existingTransaction =
                paymentTransactionRepository
                        .findFirstByPaymentOrder_OrderIdOrderByIdDesc(existingOrder.getOrderId())
                        .orElse(null);

        //handling of an order that hasn't finished processing
        if (existingTransaction == null
                || existingTransaction.getPaymentSessionId() == null
                || existingTransaction.getPaymentSessionId().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment request is still being processed. Retry with the same key."
            );
        }

        //retrieving existing information and returning it.
        return CreatePaymentResponse.builder()
                .orderId(existingOrder.getOrderId())
                .orderAmount(existingOrder.getOrderAmount())
                .orderStatus(existingOrder.getOrderStatus())
                .paymentSessionId(existingTransaction.getPaymentSessionId())
                .build();
    }

    public CreatePaymentResponse createOrder(CreatePaymentRequest request, UUID idempotencyKey)
    {

        // Check whether this idempotency key was already used
        PaymentOrder existingPaymentOrder = paymentOrderRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existingPaymentOrder != null) {
            return buildExistingPaymentResponse(existingPaymentOrder, request);
        }



        //Creating a payment order on our system
        PaymentOrder paymentOrder= PaymentOrder.builder()
                .orderId(UUID.randomUUID().toString())
                .idempotencyKey(idempotencyKey)
                .orderAmount(request.orderAmount())
                .orderCurrency(request.orderCurrency())
                .customerId(request.customerId())
                .customerPhone(request.customerPhone())
                .customerEmail(request.customerEmail())
                .orderStatus("CREATED")
                .build();
        //saving it
        //PaymentOrder savedPaymentOrder = paymentOrderRepository.save(paymentOrder);
        PaymentOrder savedPaymentOrder;

        try {
            savedPaymentOrder =
                    paymentOrderRepository.saveAndFlush(paymentOrder);

        } catch (DataIntegrityViolationException exception) {

            PaymentOrder concurrentlyCreatedOrder = paymentOrderRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);

            return buildExistingPaymentResponse(
                    concurrentlyCreatedOrder,
                    request
            );

        }

        //Cashfree requires customer information to create a payment order.
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
                            idempotencyKey,
                            null
                    );

            OrderEntity cashfreeOrder = cashfreeResponse.getData();
            if (cashfreeOrder == null
                    || cashfreeOrder.getPaymentSessionId() == null
                    || cashfreeOrder.getPaymentSessionId().isBlank()) {

                throw new IllegalStateException(
                        "Cashfree returned an invalid payment session."
                );
            }


            //setting payment session id returned from cashfree
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
        //Check wether the order is marked as PAID so that we dont repeat verification
        if ("PAID".equals(paymentOrder.getOrderStatus())) {
            return;
        }

        //Fetch payment attempts from Cashfree.
        List<PaymentEntity> payments =
                fetchPaymentsFromCashfree(orderId);

        if (payments == null || payments.isEmpty()) {
            log.warn("No payment attempts returned by Cashfree for orderId={}", orderId);
            return;
        }

        log.info("Cashfree returned {} payment attempts for orderId={}",
                payments.size(), orderId);

        payments.stream()
                .filter(Objects::nonNull)
                .forEach(payment ->
                        log.info("Payment status received from Cashfree: {}",
                                payment.getPaymentStatus())
                );

        //Find a successful payment.
        Optional<PaymentEntity> successfulPaymentOptional = payments.stream()
                .filter(Objects::nonNull)
                .filter(payment ->
                        payment.getPaymentStatus()==PaymentStatusEnum.SUCCESS)
                .findFirst();
        if(successfulPaymentOptional.isEmpty())
        {
            log.warn("No successful payment found for orderId={}", orderId);
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

        paymentStatusUpdater.markPaymentSuccessful(
                orderId,
                successfulPayment.getCfPaymentId(),
                successfulPayment.getPaymentGroup());

        log.info("Payment successfully verified and updated for orderId={}", orderId);
    }

}
