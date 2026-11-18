package com.seema.cashfree_payment_integration.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cashfree")
public class CashfreeProperties {

    private String clientId;
    private String clientSecret;
    private String environment;
    private String apiVersion;
}
