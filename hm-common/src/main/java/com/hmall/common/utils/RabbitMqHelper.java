package com.hmall.common.utils;

import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFutureCallback;


@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqHelper {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange, String routingKey, Object msg){
        rabbitTemplate.convertAndSend(exchange,routingKey,msg);
    }

    public void sendDelayMessage(String exchange, String routingKey, Object msg, int delay){
        rabbitTemplate.convertAndSend(exchange,routingKey,msg,message -> {
            message.getMessageProperties().setDelay(delay);
            return message;
        });
    }

    public void sendMessageWithConfirm(String exchange, String routingKey, Object msg, int maxRetries){
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString(true));
        correlationData.getFuture().addCallback(new ListenableFutureCallback<CorrelationData.Confirm>() {
            int retryCount = 0;
            @Override
            public void onFailure(Throwable ex) {
                log.error(ex.getMessage(),ex);
            }

            @Override
            public void onSuccess(CorrelationData.Confirm result) {
                if(result!=null && !result.isAck()){
                    log.error("消息发送失败，收到nack,已重试次数: {}",retryCount++);
                    if(retryCount>=maxRetries){
                        log.error("消息发送失败，重试次数已经达到上限: {}",maxRetries);
                        return;
                    }
                }
                CorrelationData cd = new CorrelationData(UUID.randomUUID().toString(true));
                cd.getFuture().addCallback(this);
                rabbitTemplate.convertAndSend(exchange,routingKey,msg,cd);
            }
        });

        rabbitTemplate.convertAndSend(exchange,routingKey,msg,correlationData);
    }
}
