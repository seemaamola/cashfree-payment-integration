package com.seema.cashfree_payment_integration.controller;

import com.seema.cashfree_payment_integration.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/cashfree")
    public ResponseEntity<Void> handleCashfreeWebhook(
            @RequestHeader("x-webhook-signature") String signature,
            @RequestHeader("x-webhook-timestamp") String timestamp,
            @RequestBody String rawBody){

        webhookService.processWebhook(signature,timestamp,rawBody);

        return ResponseEntity.ok().build();
    }

}
