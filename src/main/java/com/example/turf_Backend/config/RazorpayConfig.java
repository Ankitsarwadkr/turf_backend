package com.example.turf_Backend.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {
    @Value("${razorpay.key}")
    private String key;

    @Value("${razorpay.secret}")
    private String secret;

    @Bean
    public RazorpayClient razorpayClient(){
        try{
            return new RazorpayClient(key,secret);
        }catch (Exception e){
            throw new  IllegalStateException("Failed to initialize RazorpayClient ",e);
        }
    }
}
