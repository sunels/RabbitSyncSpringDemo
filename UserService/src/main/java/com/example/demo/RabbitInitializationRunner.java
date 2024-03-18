package com.example.demo;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class RabbitInitializationRunner implements ApplicationRunner {

    private final RabbitAdmin rabbitAdmin;

    public RabbitInitializationRunner(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        cleanupExistingComponents();
        declareExchangeIfNotExists();
        declareQueueIfNotExists("user_balance_request_queue");
        declareQueueIfNotExists("user_balance_response_queue");
        bindQueuesToExchange();
        //addBalanceForUser("123"); // Example user ID
    }

    private void cleanupExistingComponents() {
        deleteExchangeIfExists("user_balance_exchange");
        deleteQueueIfExists("user_balance_request_queue");
        deleteQueueIfExists("user_balance_response_queue");
    }

    private void deleteExchangeIfExists(String exchangeName) {
        if (exchangeExists(exchangeName)) {
            try {
                rabbitAdmin.deleteExchange(exchangeName);
            } catch (Exception e) {
                System.out.println("Unable to delete exchange!!!");
            }
        }
    }

    private void deleteQueueIfExists(String queueName) {
        if (queueExists(queueName)) {
            rabbitAdmin.purgeQueue(queueName, true); // Force purge
            rabbitAdmin.deleteQueue(queueName);
        }
    }

    private void declareExchangeIfNotExists() {
        if (!exchangeExists("user_balance_exchange")) {
            Exchange exchange = ExchangeBuilder.fanoutExchange("user_balance_exchange").durable(true).build();
            rabbitAdmin.declareExchange(exchange);
        }
    }

    private void declareQueueIfNotExists(String queueName) {
        if (!queueExists(queueName)) {
            try {
                Queue queue = QueueBuilder.durable(queueName).build();
                rabbitAdmin.declareQueue(queue);
            } catch (Exception e) {
                System.out.println(" Unable to declare the QUEUE: " + queueName);
            }
        }
    }

    private boolean exchangeExists(String exchangeName) {
        try {
            return false;//rabbitAdmin.getExchangeProperties(exchangeName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean queueExists(String queueName) {
        try {
            return rabbitAdmin.getQueueProperties(queueName) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void bindQueuesToExchange() {
        String exchangeName = "user_balance_exchange";
        Binding requestBinding = new Binding("user_balance_request_queue", Binding.DestinationType.QUEUE, exchangeName, "user_balance_routing_key", null);
        rabbitAdmin.declareBinding(requestBinding);

        Binding responseBinding = new Binding("user_balance_response_queue", Binding.DestinationType.QUEUE, exchangeName, "user_balance_response_routing_key", null);
        rabbitAdmin.declareBinding(responseBinding);
    }

    private void addBalanceForUser(String userId) {
        // Assuming "user_balance_response_queue" is used to store user balances
        // This is where you'd implement the logic to add a balance for the user.
        // This could involve sending a message to the queue with the user ID and initial balance.
        String message = "Initial balance for user " + userId; // Simplified message, adjust according to your application's needs
        rabbitAdmin.getRabbitTemplate().convertAndSend("user_balance_response_queue", message);
        System.out.println("Added initial balance for user " + userId);
    }

}
