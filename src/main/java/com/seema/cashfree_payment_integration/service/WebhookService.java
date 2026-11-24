package com.seema.cashfree_payment_integration.service;

import com.seema.cashfree_payment_integration.entity.PaymentOrder;
import com.seema.cashfree_payment_integration.properties.CashfreeProperties;
import com.seema.cashfree_payment_integration.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;


@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final CashfreeProperties cashfreeProperties;
    private final ObjectMapper objectMapper;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentStatusUpdater paymentStatusUpdater;

    public void processWebhook(String signature, String timestamp, String rawBody) {
        log.info("Received cashfree Webhook");

        //Verify the sender
        if (!verifySignature(signature, timestamp, rawBody)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Cashfree webhook signature"
            );
        }

        log.info("Cashfree webhook signature verified successfully");

        // Parse the verified JSON
        try {
            JsonNode event = objectMapper.readTree(rawBody);

            // Extract event type
            String eventType = event.path("type").asText(null);

            if (eventType == null || eventType.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing webhook event type"
                );
            }

            if (!"PAYMENT_SUCCESS_WEBHOOK".equals(eventType)) {
                log.info("Ignoring unsupported webhook event: {}", eventType);
                return;
            }

            log.info("Verified Cashfree webhook event type: {}", eventType);

            // Get the order and payment objects
            JsonNode order = event.path("data").path("order");
            JsonNode payment = event.path("data").path("payment");

            // Extract payment details
            String orderId = order.path("order_id").asText(null);

            String cfPaymentId =
                    payment.path("cf_payment_id").asText(null);

            String paymentStatus =
                    payment.path("payment_status").asText(null);

            String paymentCurrency =
                    payment.path("payment_currency").asText(null);

            // Extract amount without losing decimal precision
            JsonNode amountNode = payment.path("payment_amount");

            BigDecimal paymentAmount =
                    amountNode.isNumber() ? amountNode.decimalValue() : null;


            if (orderId == null || orderId.isBlank()
                    || cfPaymentId == null || cfPaymentId.isBlank()
                    || paymentStatus == null || paymentStatus.isBlank()
                    || paymentCurrency == null || paymentCurrency.isBlank()
                    || paymentAmount == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing or invalid payment details in webhook"
                );
            }

            PaymentOrder paymentOrder = paymentOrderRepository
                    .findByOrderId(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Payment order not found"
                    ));

            if (paymentOrder.getOrderAmount().compareTo(paymentAmount) != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment amount mismatch"
                );
            }

            if (!paymentOrder.getOrderCurrency().equalsIgnoreCase(paymentCurrency)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Payment currency mismatch"
                );
            }

            if (!"SUCCESS".equalsIgnoreCase(paymentStatus)) {
                log.info(
                        "Payment is not successful for order {}. Status: {}",
                        orderId,
                        paymentStatus
                );
                return;
            }

            String paymentGroup =
                    payment.path("payment_group").asText(null);

            if (paymentGroup == null || paymentGroup.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Missing payment method"
                );
            }



            // Update the database only after all validations
            paymentStatusUpdater.markPaymentSuccessful(
                    orderId,
                    cfPaymentId,
                    paymentGroup
            );

            log.info("Payment successfully updated for order: {}", orderId);



        } catch (JacksonException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid webhook JSON",
                    exception);
        }
    }

    private boolean verifySignature(
            String signature,
            String timestamp,
            String rawBody) {

        if (signature == null || signature.isBlank()
                || timestamp == null || timestamp.isBlank()
                || rawBody == null) {
            return false;
        }

        //Spring injects our existing CashfreeProperties object.
        //retrieve the Client secret
        String clientSecret = cashfreeProperties.getClientSecret();

        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "Cashfree client secret is not configured"
            );
        }

        try {

            //This tells which cryptographic algorithm to use.
            Mac mac = Mac.getInstance("HmacSHA256");

            //Your Cashfree client secret becomes the key used to calculate the signature.
            SecretKeySpec secretKey = new SecretKeySpec(
                    clientSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");

            mac.init(secretKey);

            //Creating the signed payload
            String signedPayload = timestamp + rawBody;

            byte[] calculatedSignature = mac.doFinal(
                    signedPayload.getBytes(StandardCharsets.UTF_8)
            );

            byte[] receivedSignature;

            try {
                receivedSignature = Base64.getDecoder().decode(signature);
            } catch (IllegalArgumentException exception) {
                return false;
            }

            //compares the actual signature bytes rather than trusting the incoming signature.
            return MessageDigest.isEqual(
                    calculatedSignature,
                    receivedSignature
            );

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to verify Cashfree webhook signature",
                    exception
            );
        }
    }
}
