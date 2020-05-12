package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @author mqx
 * 监听消息,获取哪些商品是秒杀商品，并将商品添加到缓存！
 * @date 2020/5/11 9:33
 */
@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    // 编写监听消息的方法,并将商品添加到缓存！
    @SneakyThrows
    @RabbitListener(bindings =@QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importGoodsToRedis(Message message, Channel channel){
        // 获取秒杀商品？什么样的商品算是秒杀商品：审核状态status=1，startTime：new Date()当天
        // 根据上述定义查询所有秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        // 导入时间比较的工具类。时间比较只取年月日
        seckillGoodsQueryWrapper.eq("status",1).eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        // 有秒杀商品
        if (null!=seckillGoodsList && seckillGoodsList.size()>0){
            // 循环秒杀商品放入缓存
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                // 在放入秒杀商品之前，先判断缓存中是否已经存在，如果存在，那么就不需要放入了。
                // hset(key,field,value);
                // key = seckill:goods ,field = skuId ,value = 秒杀商品对象
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                // 当前缓存中有秒杀商品
                if (flag){
                    // 不放入数据
                    continue;
                }
                // 缓存中没有数据，秒杀商品放入缓存。
                // 25 -- 100
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);
                // 分析商品数量如何存储？如何防止库存超卖?
                // 使用redis 中 list 的数据类型 redis --- 单线程
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    // key = seckill:stock:skuId
                    // value = skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                // 将所有的商品状态位初始化为1 ：状态位只有位1的时候，那么这个商品才能秒，如果为0，不能秒。
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
            }
            // 手动确认消息已被处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    // 监听用户发送过来的消息。
    //  rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillGoods(UserRecode userRecode,Message message,Channel channel){
        // 判断用户信息不能位空
        if(null!=userRecode){
            // 预下单处理。
            seckillGoodsService.seckillOrder(userRecode.getSkuId(),userRecode.getUserId());

            // 手动确认消息被处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }

    }
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void delseckillGoods(Message message,Channel channel){
        // 删除操作
        // 查询结束的秒杀商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        // 结束时间
        seckillGoodsQueryWrapper.eq("status",1).le("end_time",new Date());
        // 获取到结束的秒杀商品
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);
        if (!CollectionUtils.isEmpty(seckillGoodsList)){
            // 删除缓存数据
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                // 删除缓存的秒杀商品的数量
                redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
            }
        }
        // 继续删除
        // seckill:goods 存储的所有秒杀商品的数据
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        // seckill:orders:users
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

        // 变更数据库状态
        // status = 1 , status =2
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods,seckillGoodsQueryWrapper);

        // 手动确认消息已经被消费
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
