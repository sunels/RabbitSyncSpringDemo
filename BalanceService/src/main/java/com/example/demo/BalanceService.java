package com.example.demo;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = "user_balance_request_queue")
    public void processBalanceRequest(UserRequest userRequest, Message requestMessage) {
        System.out.println("GOT userRequest = " + userRequest);
        // Simulate processing the balance request
        // In a real scenario, this could involve querying a database or external service
        String userBalance = "Balance for user " + userRequest.getUserId() + ": $123"; // Example balance

        // Construct the response object
        UserBalanceResponse response = new UserBalanceResponse(userRequest.getUserId(), userBalance);

        // Send the response back to the UserController using the replyTo queue specified in the request message
        rabbitTemplate.convertAndSend(requestMessage.getMessageProperties().getReplyTo(), response, message -> {
            message.getMessageProperties().
                    setCorrelationId(
                            requestMessage.getMessageProperties().getCorrelationId()
                    );
            return message;
        });
    }

}


