package com.example.demo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BalanceServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BalanceService balanceService;

    public BalanceServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessBalanceRequestSendsResponse() {
        UserRequest userRequest = new UserRequest();
        userRequest.setUserId("user1");
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setReplyTo("replyQueue");
        messageProperties.setCorrelationId("corrId123");
        Message message = new Message(new byte[0], messageProperties);

        balanceService.processBalanceRequest(userRequest, message);

        ArgumentCaptor<UserBalanceResponse> responseCaptor = ArgumentCaptor.forClass(UserBalanceResponse.class);
        ArgumentCaptor<String> queueCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate, times(1)).convertAndSend(queueCaptor.capture(), responseCaptor.capture(), any(org.springframework.amqp.core.MessagePostProcessor.class));
        assertEquals("replyQueue", queueCaptor.getValue());
        assertEquals("user1", responseCaptor.getValue().getUserId());
        assertTrue(responseCaptor.getValue().getUserBalance().contains("user1"));
    }
}
