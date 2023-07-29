package com.seshrao.stockxpress.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean  // A bean of type 'WebClient' will be created with the method name 'webClient'
    public WebClient webClient(){
        return WebClient.builder().build() ;
    }
}
