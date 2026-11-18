package com.seema.cashfree_payment_integration.config;

import com.cashfree.pg.Cashfree;
import com.seema.cashfree_payment_integration.properties.CashfreeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CashfreeProperties.class)
public class CashfreeConfig {


    @Bean
    public Cashfree cashfree(CashfreeProperties properties)
    {
        Cashfree.CFEnvironment environment = Cashfree.CFEnvironment.valueOf(properties.getEnvironment().toUpperCase());

        return new Cashfree(
                environment,
                properties.getClientId(),
                properties.getClientSecret(),
                null,
                null,
                null);
    }
}
