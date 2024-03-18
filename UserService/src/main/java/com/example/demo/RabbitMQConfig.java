package com.example.demo;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.demo.UserApplication.INSTANCE_IDENTITY;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public Exchange userBalanceExchange() {
        return ExchangeBuilder.directExchange("user_balance_exchange")
                .durable(true)  // Make the exchange durable
                .build();
    }
    @Bean
    public Queue userBalanceRequestQueue() {
        return new Queue("user_balance_request_queue", true); // Make the queue durable
    }
    @Bean
    public Queue userBalanceResponseQueue() {
        return new Queue("user_balance_response_queue_" + INSTANCE_IDENTITY, true); // Make the queue durable
    }
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(userBalanceRequestQueue())
                .to(userBalanceExchange())
                .with("user_balance_routing_key")
                .noargs();
    }
    @Bean
    public Binding bindingResponse() {
        return BindingBuilder.bind(userBalanceResponseQueue())
                .to(userBalanceExchange())
                .with("user_balance_response_routing_key")
                .noargs();
    }


}
