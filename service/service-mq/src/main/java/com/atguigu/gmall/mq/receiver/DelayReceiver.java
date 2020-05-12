package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * 消息监听
 * @date 2020/5/6 11:44
 */
@Component
@Configuration
public class DelayReceiver {

    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getMsg(String msg){
        System.out.println("接收数据："+msg);
        // 声明一个时间格式对象。
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 看接收到的消息数据：
        System.out.println("Receive queue_delay_1: " + simpleDateFormat.format(new Date()) + " Delay rece." + msg);

    }
}
