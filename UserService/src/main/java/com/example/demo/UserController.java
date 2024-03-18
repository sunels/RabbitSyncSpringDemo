package com.example.demo;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RestController
public class UserController {

    private final RabbitTemplate rabbitTemplate;
    private final Map<String, CountDownLatch> correlationIdLatchMap = new ConcurrentHashMap<>();
    private final Map<String, UserBalanceResponse> correlationIdResponseMap = new ConcurrentHashMap<>();
    private final DynamicQueueNameResolver dynamicQueueNameResolver;

    public UserController(RabbitTemplate rabbitTemplate, DynamicQueueNameResolver dynamicQueueNameResolver) {
        this.rabbitTemplate = rabbitTemplate;
        this.dynamicQueueNameResolver = dynamicQueueNameResolver;
    }

    @RabbitListener(queues = "#{dynamicQueueNameResolver.resolveResponseQueueName()}")
    public void handleResponse(@Payload UserBalanceResponse response, org.springframework.amqp.core.Message message) {
        System.out.println("UserController Got a rabbit message = " + response);
        String correlationId = message.getMessageProperties().getCorrelationId();
        correlationIdResponseMap.put(correlationId, response);
        CountDownLatch latch = correlationIdLatchMap.get(correlationId);
        if (latch != null) {
            latch.countDown();
        }
    }

    @PostMapping("/get-user-balance")
    public ResponseEntity<String> getUserBalance(@RequestBody UserRequest userRequest) throws InterruptedException {
        // Generate a correlation ID for the request
        String correlationId = UUID.randomUUID().toString();
        // Setup latch to wait for the response
        CountDownLatch latch = new CountDownLatch(1);
        correlationIdLatchMap.put(correlationId, latch);

        // Send request to the BalanceService with the correlation ID
        rabbitTemplate.convertAndSend("user_balance_request_queue", userRequest, message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            // Set replyTo to a dynamic queue based on correlation ID
            message.getMessageProperties().setReplyTo(dynamicQueueNameResolver.resolveResponseQueueName());
            return message;
        });
        System.out.println("UserController has SENT a rabbit message = " + userRequest);

        // Wait for response with a timeout
        latch.await(5, TimeUnit.SECONDS);
        correlationIdLatchMap.remove(correlationId);
        UserBalanceResponse userBalance = correlationIdResponseMap.remove(correlationId);

        if (userBalance != null) {
            return ResponseEntity.ok("User balance for user ID " + userRequest.getUserId() + " is: " + userBalance);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to get user balance for user ID: " + userRequest.getUserId());
        }
    }
}