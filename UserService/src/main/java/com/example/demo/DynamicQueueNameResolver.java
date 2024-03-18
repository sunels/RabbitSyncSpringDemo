package com.example.demo;

import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.example.demo.UserApplication.INSTANCE_IDENTITY;

@Component
public class DynamicQueueNameResolver {
    public String resolveResponseQueueName() {
        // Generate a unique identifier, e.g., random string
        return "user_balance_response_queue_" + INSTANCE_IDENTITY;
    }
}