package com.hmall.trade.listener;

import com.hmall.api.client.PayClient;
import com.hmall.api.dto.PayOrderDTO;
import com.hmall.common.constants.MqConstants;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderDelayListener {
    private final IOrderService orderService;
    private final PayClient payClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstants.DELAY_ORDER_QUEUE_NAME,durable = "true"),
            exchange = @Exchange(name = MqConstants.DELAY_EXCHANGE_NAME,delayed = "true"),
            key = MqConstants.DELAY_ORDER_KEY
    ))
    public void orderDelayListener(Long orderId){
        //查询本地订单状态
        Order order = orderService.getById(orderId);
        //如果订单状态为已支付
        if(order !=null && order.getStatus() ==2){
            return;
        }
        //如果订单状态为未支付
        //查询订单流水
        PayOrderDTO payOrderDTO = payClient.queryPayOrderByBizOrderNo(orderId);

        if(payOrderDTO !=null && payOrderDTO.getStatus()==3){
            //如果订单流水为已支付状态，则更新本地订单状态
            orderService.markOrderPaySuccess(orderId);
        }else{
            //如果为未支付状态，取消订单
            orderService.cancelOrder(orderId);
        }

    }
}
