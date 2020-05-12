package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * @date 2020/5/11 11:20
 */
@Service
public class SeckillGoodsServiceImpl  implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public List<SeckillGoods> findAll() {
        // 每天夜晚扫描发送消息，消费消息将数据放入缓存！
        return redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
    }

    @Override
    public SeckillGoods getSeckillGoodsById(Long skuId) {
        return (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
    }

    @Override
    public void seckillOrder(Long skuId, String userId) {
        /*
            a.	监听用户，同一个用户不能抢两次，
            b.	判断状态位，
            c.	监听商品库存数量{redis-list}
            d.	将用户秒杀记录放入缓存。
         */
        // 判断状态
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)){
            // 没有商品了，你走吧
            return;
        }
        // 如何保证用户不能抢多次，
        // 如果用户第一次抢到了，那么就会将抢到的信息存储在缓存中， 利用redis --- setnx (); 判断key 是否存在。
        // userSeckillKey = seckill:user:userId
        String userSeckillKey = RedisConst.SECKILL_USER+userId;
        // 如果执行成功返回true，说明第一次添加key，如果返回false, 执行失败。不是第一次添加key
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(userSeckillKey, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!isExist){
            return;
        }
        // 用户可以下单，减少库存。
        // 添加商品数量的时候 redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        if (StringUtils.isEmpty(goodsId)){
            // 如果没有吐出来，那么就说明已经售罄
            // 通知其他兄弟节点，当前商品没有了，更新内存中的状态位
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            return;
        }
        // 记录订单 ， 做一个秒杀的订单类。
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        // 通过skuId ,查询用户秒杀哪个商品
        orderRecode.setSeckillGoods(getSeckillGoodsById(skuId));
        orderRecode.setNum(1);
        orderRecode.setOrderStr(MD5.encrypt(userId));

        // 将用户秒杀的订单放入缓存。
        // orderSeckillKey=seckill:orders
        String orderSeckillKey = RedisConst.SECKILL_ORDERS;
        redisTemplate.boundHashOps(orderSeckillKey).put(orderRecode.getUserId(),orderRecode);

        // 更新商品的数量
        updaetStockCout(orderRecode.getSeckillGoods().getSkuId());
    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        /*
        response.data.code == 211	show == 1   排队中...
        response.data.code == xxx	show == 2   {{message}}
        response.data.code == 215	show == 3   抢购成功 <a href="/seckill/trade.html" target="_blank">去下单</a>
        response.data.code == 218	show == 4   下订单成功 <a href="http://order.gmall.com/myOrder.html" target="_blank">我的订单</a>
         */
        // 判断用户是否存在，用户不能购买两次
        // 用户是否能够抢单！ 215
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if (isExist){
            // 有seckill:user:userId 用户key
            // 判断订单是否存在
            Boolean isHashKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (isHashKey){
                // 抢单成功,获取用户的订单对象
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                // 返回数据 用户抢单成功！
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        // 判断用户是否下过订单
        // key = seckill:orders:users field = userId value 订单的数据。
        Boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        // 如果返回true 说明执行成功， 218
        if (isExistOrder){
            // 获取订单Id
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);
            // 应该是第一次下单成功
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }
        // 判断状态位
        String status = (String) CacheHelper.get(skuId.toString());
        // status =1 表示能够抢单，如果是 0 抢单失败，已经售罄
        if ("0".equals(status)){
            return Result.build(null,ResultCodeEnum.SECKILL_FINISH);
        }
        // 默认情况下
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    // 更新商品的数量。
    private void updaetStockCout(Long skuId) {
        // 秒杀商品的库存 在缓存中有一份，在数据库中也有一份。
        Long count = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        // 主要目的是不想频繁更新数据。
        if (count%2==0){
            // 获取缓存中当前的秒杀商品
            SeckillGoods seckillGoods = getSeckillGoodsById(skuId);
            seckillGoods.setStockCount(count.intValue());
            // 更新数据库
            seckillGoodsMapper.updateById(seckillGoods);

            // 更新缓存
            // key = seckill:goods
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(),seckillGoods);
        }
    }
}
