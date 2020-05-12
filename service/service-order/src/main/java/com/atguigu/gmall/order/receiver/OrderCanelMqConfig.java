package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author mqx
 * 编写取消订单的配置类
 * @date 2020/5/6 14:12
 */
@Configuration
public class OrderCanelMqConfig {

    // 先声明一个队列
    @Bean
    public Queue delayQueue(){
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true);
    }

    // 定义交换机
    @Bean
    public CustomExchange delayExchange(){
        // 配置参数
        HashMap<String, Object> map = new HashMap<>();
        // 基于插件时，需要指定的参数，固定的用法。
        map.put("x-delayed-type","direct");
        // 基于插件的交换机类型这个key是固定值。
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,map);
    }
    // 绑定交换机与队列
    @Bean
    public Binding delayBinding(){
        // 返回绑定结果
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }

}
