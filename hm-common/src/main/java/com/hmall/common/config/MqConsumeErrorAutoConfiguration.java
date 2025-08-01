package com.hmall.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "spring.rabbitmq.listener.simple.retry.enabled", havingValue = "true")
public class MqConsumeErrorAutoConfiguration {
    @Value("${spring.application.name}")
    private String SERVICE_NAME;

    @Bean
    public DirectExchange errorExchange() {
        return new DirectExchange("error.direct");
    }

    @Bean
    public Queue errorQueue(){
        return new Queue(SERVICE_NAME+".error.queue");
    }

    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange errorExchange){
        return BindingBuilder.bind(errorQueue).to(errorExchange).with("error");
    }

    public MessageRecoverer errorMessageRecover(RabbitTemplate rabbitTemplate){
        return new RepublishMessageRecoverer(rabbitTemplate,"error.direct","error");
    }

}
