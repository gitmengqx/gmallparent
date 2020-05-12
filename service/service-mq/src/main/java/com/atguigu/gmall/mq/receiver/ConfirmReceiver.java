package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author mqx
 * 接收处理消息
 * @date 2020/5/5 15:44
 */
@Configuration
@Component
public class ConfirmReceiver {

    // 消费消息 ，必须知道你的exchange，同时还需要知道routingKey，获取队列。
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}
    ))
    public void confirmMessage(Message message, Channel channel){
        // 获取里面的数据
        // 将字节数组转化为字符串
        String str = new String(message.getBody());
        System.out.println("接收到的消息："+str);

        // 第一个参数：long 类型的Id，
        // 确认消息个形式：false 表示每次确认一个消息，true 表示批量确认
        try {
            // 如果有异常：
            int i = 1/0;
            // ack表示消息正确处理，手动签收了。
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            System.out.println("出现异常！");
            // 判断消息是否已经处理过一次 ，如果消息已经处理过一次，则返回true，如果消息一次没有处理则是false。
            if(message.getMessageProperties().getRedelivered()){
                System.out.println("消息已经处理过了。");
                // 表示消息不重回队列。
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            }else {
                // nack 消息接收了，但是没有正确处理。
                System.out.println("消息即将返回队列！");
                // 第三个参数表示如果消息没有正确处理，消息会再次回到消息队列。
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }


        }
    }
}
