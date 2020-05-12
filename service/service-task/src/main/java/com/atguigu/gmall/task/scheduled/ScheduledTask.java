package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author mqx
 * @date 2020/5/11 9:23
 */
@Component
@EnableScheduling // 表示开启定时任务。
public class ScheduledTask {

    @Autowired
    private RabbitService rabbitService;

    // 定义任务开启时间 凌晨1点钟
    // @Scheduled(cron = "0 0 1 * * ?")
    // 每个30秒触发当前任务
    @Scheduled(cron = "0/30 * * * * ?")
    public void tsak(){
        // 发送消息
        // 发送的内容，是空。处理消息的时候扫描秒杀商品！
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");
    }

    // 每个30秒触发当前任务 每天晚上18删除 根据实际情况去删除。。。
    @Scheduled(cron = "* * 18 * * ?")
    public void tsakDelRedis(){
        // 发送消息
        // 发送的内容，是空。处理消息的时候扫描秒杀商品！
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_18,"");
    }
}
