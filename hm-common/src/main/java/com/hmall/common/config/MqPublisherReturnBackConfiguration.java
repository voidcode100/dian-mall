package com.hmall.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
@ConditionalOnClass(RabbitTemplate.class)
@RequiredArgsConstructor
public class MqPublisherReturnBackConfiguration {
    private final RabbitTemplate rabbitTemplate;
    @PostConstruct
    public void init(){
        rabbitTemplate.setReturnsCallback(returnedMessage -> {
            log.error("监听到消息return callback");
            log.debug("exchange: {}",returnedMessage.getExchange());
            log.debug("routingKey: {}",returnedMessage.getRoutingKey());
            log.debug("message: {}",returnedMessage.getMessage());
            log.debug("replyCode: {}",returnedMessage.getReplyCode());
            log.debug("replyText: {}",returnedMessage.getMessage());
        });
    }
}
