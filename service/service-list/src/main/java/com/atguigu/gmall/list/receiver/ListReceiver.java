package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mqx
 * 监听service-product 发过来的消息
 * @date 2020/5/6 9:23
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;
    /**
     * 监听商品上架：调用upperGoods() 方法
     * rabbitmq 监听消息可以使用注解。
     * durable = "true",type = ExchangeTypes.DIRECT 可以不用写，默认
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel){
        // 判断skuId不能为空
        if (null!=skuId){
            // 有商品Id，则调用商品的上架操作，将数据从mysql --- 上传到es
            searchService.upperGoods(skuId);
        }
        // 手动签收确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 监听商品的下架
     * @param skuId 商品Id
     * @param message 消息
     * @param channel 管道
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel){
        // 判断skuId不能为空
        if (null!=skuId){
            // 有商品Id，则调用商品的上架操作，将数据从mysql --- 上传到es
            searchService.lowerGoods(skuId);
        }
        // 手动签收确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
