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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RestController
public class UserController {

    private final RabbitTemplate rabbitTemplate;
    private final DynamicQueueNameResolver dynamicQueueNameResolver;
    private final Map<String, CompletableFuture<UserBalanceResponse>> correlationIdFutureMap = new ConcurrentHashMap<>();

    public UserController(RabbitTemplate rabbitTemplate, DynamicQueueNameResolver dynamicQueueNameResolver) {
        this.rabbitTemplate = rabbitTemplate;
        this.dynamicQueueNameResolver = dynamicQueueNameResolver;
    }

    @RabbitListener(queues = "#{dynamicQueueNameResolver.resolveResponseQueueName()}")
    public void handleResponse(@Payload UserBalanceResponse response, org.springframework.amqp.core.Message message) {
        System.out.println("UserController Got a rabbit message = " + response);
        String correlationId = message.getMessageProperties().getCorrelationId();
        CompletableFuture<UserBalanceResponse> future = correlationIdFutureMap.get(correlationId);
        if (future != null) {
            future.complete(response);
        }
    }

    @PostMapping("/get-user-balance")
    public ResponseEntity<String> getUserBalance(@RequestBody UserRequest userRequest) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<UserBalanceResponse> future = new CompletableFuture<>();
        correlationIdFutureMap.put(correlationId, future);

        rabbitTemplate.setReplyAddress(dynamicQueueNameResolver.resolveResponseQueueName());

        rabbitTemplate.convertAndSend("user_balance_request_queue", userRequest, message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            message.getMessageProperties().setReplyTo(dynamicQueueNameResolver.resolveResponseQueueName());
            return message;
        });

        System.out.println("UserController has SENT a rabbit message = " + userRequest);

        return future.thenApplyAsync(userBalance -> {
                    if (userBalance != null) {
                        return ResponseEntity.ok("User balance for user ID " + userRequest.getUserId() + " is: " + userBalance);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Failed to get user balance for user ID: " + userRequest.getUserId());
                    }
                }).exceptionally(ex -> {
                    // Handle exceptions, if any
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to get user balance for user ID: " + userRequest.getUserId());
                }).orTimeout(5, TimeUnit.SECONDS) // Timeout duration
                .whenComplete((responseEntity, throwable) -> {
                    correlationIdFutureMap.remove(correlationId);
                }).join();
    }
}