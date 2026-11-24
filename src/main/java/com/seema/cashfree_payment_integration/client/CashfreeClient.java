package com.seema.cashfree_payment_integration.client;

import com.cashfree.pg.Cashfree;
import com.cashfree.pg.ApiException;
import com.cashfree.pg.ApiResponse;
import com.cashfree.pg.model.OrderCreateRefundRequest;
import com.cashfree.pg.model.RefundEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class CashfreeClient {

    private final Cashfree cashfree;

    public RefundEntity createRefund(
            String orderId,
            String refundId,
            BigDecimal refundAmount,
            String refundNote
    ) {

        OrderCreateRefundRequest request = new OrderCreateRefundRequest();

        request.setRefundId(refundId);
        request.setRefundAmount(BigDecimal.valueOf(refundAmount.doubleValue()));
        request.setRefundNote(refundNote);

        try {
            ApiResponse<RefundEntity> response =
                    cashfree.PGOrderCreateRefund(
                            orderId,
                            request,
                            null,
                            null,
                            null
                    );

            return response.getData();

        } catch (ApiException exception) {
            throw new RuntimeException(
                    "Cashfree refund API request failed",
                    exception
            );
        }
    }


    public RefundEntity fetchRefund(
            String orderId,
            String refundId
    ) {
        try {
            ApiResponse<RefundEntity> response =
                    cashfree.PGOrderFetchRefund(
                            orderId,
                            refundId,
                            null,
                            null,
                            null
                    );

            return response.getData();

        } catch (ApiException exception) {
            throw new RuntimeException(
                    "Unable to fetch refund status from Cashfree",
                    exception
            );
        }
    }



}
