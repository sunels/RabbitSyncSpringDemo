package com.example.demo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private DynamicQueueNameResolver dynamicQueueNameResolver;

    @InjectMocks
    private UserController userController;

    public UserControllerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserBalanceSendsMessage() {
        UserRequest request = new UserRequest("user1", "Test User", "test@example.com", "BALANCE", System.currentTimeMillis(), "WEB");
        when(dynamicQueueNameResolver.resolveResponseQueueName()).thenReturn("responseQueue");

        // Only verify that the message is sent, do not check the response
        userController.getUserBalance(request);
        verify(rabbitTemplate, times(1)).convertAndSend(eq("user_balance_request_queue"), eq(request), any(org.springframework.amqp.core.MessagePostProcessor.class));
    }

    @Test
    void testHandleResponseCompletesFuture() throws Exception {
        String correlationId = UUID.randomUUID().toString();
        UserBalanceResponse balanceResponse = new UserBalanceResponse("user1", "Balance: $100");
        org.springframework.amqp.core.Message message = mock(org.springframework.amqp.core.Message.class);
        org.springframework.amqp.core.MessageProperties messageProperties = mock(org.springframework.amqp.core.MessageProperties.class);
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getCorrelationId()).thenReturn(correlationId);

        // Use reflection to access the private field
        java.lang.reflect.Field field = UserController.class.getDeclaredField("correlationIdFutureMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, CompletableFuture<UserBalanceResponse>> map = (java.util.Map<String, CompletableFuture<UserBalanceResponse>>) field.get(userController);
        CompletableFuture<UserBalanceResponse> future = new CompletableFuture<>();
        map.put(correlationId, future);
        userController.handleResponse(balanceResponse, message);
        assertTrue(future.isDone());
        assertEquals(balanceResponse, future.getNow(null));
    }
}
