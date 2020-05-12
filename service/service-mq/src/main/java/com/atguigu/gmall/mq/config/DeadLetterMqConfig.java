package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author mqx
 * 变成一个配置类，指定消息延迟时间
 * 配置死信交换机的绑定关系
 * @date 2020/5/6 10:17
 */
@Configuration
public class DeadLetterMqConfig {

    // 声明一些变量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    // 声明一个交换机
    @Bean
    public DirectExchange exhange(){
        // 返回一个交换机
        return new DirectExchange(exchange_dead,true,false,null);
    }
    // 如果队列出现了问题，那么得走队列2
    @Bean
    public Queue queue1(){
        // 设置参数
        HashMap<String, Object> map = new HashMap<>();
        // 设置一个死信交换机 固定值 x-dead-letter-exchange
        map.put("x-dead-letter-exchange",exchange_dead);
        // 给当前的死信交换机绑定一个队列 通过routingkey 绑定
        // key 是固定值x-dead-letter-routing-key
        map.put("x-dead-letter-routing-key",routing_dead_2);
        // 统一规定延迟时间。
        map.put("x-message-ttl",1000*10);
        // 最后一个参数：表示是否要给当前队列设置参数，如果有参数设置则需要写在map中。
        return new Queue(queue_dead_1,true,false,false,map);
    }
    // 给单曲队列设置一个绑定关系
    @Bean
    public Binding binding(){
        // 绑定队列 将队列1 -- queue1 通过routingKey {routing_dead_1} 绑定到交换机 exhange
        return BindingBuilder.bind(queue1()).to(exhange()).with(routing_dead_1);
    }
    // 声明第二个队列 ,如果队列1 出现问题，则会走队列2
    @Bean
    public Queue queue2(){
        return new Queue(queue_dead_2,true,false,false,null);
    }

    // 将队列2同样绑定到交换机
    @Bean
    public Binding deadBinding(){
        return BindingBuilder.bind(queue2()).to(exhange()).with(routing_dead_2);
    }
}
