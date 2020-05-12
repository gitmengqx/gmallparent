package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * 消息接收方
 * @date 2020/5/6 10:51
 */
@Component
@Configuration
public class DeadLetterReceiver {

    // bindings 不用写了，因为在DeadLetterMqConfig 这个配置类中，已经绑定完成。
    // 所以此处只需要配置监听队列即可。
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getMsg(String msg){
        System.out.println("接收数据："+msg);
        // 声明一个时间格式对象。
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 看接收到的消息数据：
        System.out.println("Receive queue_dead_2: " + simpleDateFormat.format(new Date()) + " Delay rece." + msg);
    }

}
