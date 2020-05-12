package com.atguigu.gmall.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mqx
 * 发送消息的配置类
 * ConfirmCallback: 发送的确认，消息是否正确到达交换机
 * ReturnCallback： 消息没有正确到达队列触发回调如果正确到达了，这个returnedMessage 就不会执行了。
 * @date 2020/5/5 15:18
 */

@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {

    // 引入操作消息队列的模板
    @Autowired
    private  RabbitTemplate rabbitTemplate;

    // 初始化方法 当类加载的时候，先初始化确认，回调方法。传入当前类。
    @PostConstruct
    public void init(){
        // 初始化ConfirmCallback,ReturnCallback
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnCallback(this);
    }
    // 如果消息没有到到交换机则confirm会执行，返回一个ack=false 如果消息到了交换机 product--exchage ack=true
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        // 判断ack=true
        if(ack){
            log.info("消息成功发送到交换机！");
        }else {
            log.info("消息没有发送到交换机！");
        }
    }
    // 交换机与队列绑定判断
    // 如果消息从交换机正确绑定到队列，那么这个方法不会执行。
    // 如果消息从交换机没有绑定到队列，那么这个方法就会执行。
    /**
     *
     * @param message 消息的内容
     * @param replyCode 消息码
     * @param replyText 消息码对应的内容
     * @param exchange 绑定的交换机
     * @param routingKey 绑定的routingKey
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        // 交换机与队列投递失败，则会走这个方法。。。。。 在这个方法中可以 重写 rabbitmq 重试机制。 重写发送消息N次。
        // 可以利用redis 来记录发送的消息。。。。。
    }
}
