package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * @date 2020/5/5 15:40
 */
@RestController
@RequestMapping("/mq")
public class MqController {

    // 调用RabbitService.sendMessage()
    @Autowired
    private RabbitService rabbitService;
    // 引入消息队列的模板
    @Autowired
    private RabbitTemplate rabbitTemplate;
    // 发送消息控制器
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        String message = "hello RabbitMq!";
        rabbitService.sendMessage("exchange.confirm","routing.confirm",message);
        return Result.ok();
    }

    /*规定单独消息发送数据延迟时间*/
//    @GetMapping("sendDeadLettle")
//    public Result sendDeadLettle(){
//        // 声明一个时间格式对象。
//        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        // 发送一个消息 hello word，给当前的消息设置一个TTL
//        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"hello Word",
//                message -> {
//                    // 定义发送的内容以及消息的TTL 消息的存活时间10秒
//                    // 设置的延迟时间。10秒钟。
//                    message.getMessageProperties().setExpiration(1000*10+"");
//                    System.out.println(simpleDateFormat.format(new Date())+"Delay sent.");
//                    return message;
//                });
//        return Result.ok();
//    }
    /*统一规定队列中的延迟时间*/
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        // 发送数据
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"atguigu");
        System.out.println(simpleDateFormat.format(new Date())+"Delay sent.");
        return Result.ok();
    }

    @GetMapping("sendDealy")
    public Result sendDealy(){
        // 发送数据
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay,
                simpleDateFormat.format(new Date()), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 设置延迟时间
                        message.getMessageProperties().setDelay(10*1000);
                        System.out.println(simpleDateFormat.format(new Date()) + " Delay send....");
                        return message;
                    }
                });
        return Result.ok();
    }
}
