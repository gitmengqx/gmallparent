package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.sun.xml.internal.ws.resources.HttpserverMessages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author mqx
 * @date 2020/5/11 11:24
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {

    // 引入服务层
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;
    
    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;

    // 查询所有秒杀商品数据
    @GetMapping("findAll")
    public Result findAll(){
        return Result.ok(seckillGoodsService.findAll());
    }

    // 获取秒杀商品详情数据
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId){
        return Result.ok(seckillGoodsService.getSeckillGoodsById(skuId));
    }

    // skuIdStr 是下单码
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request){
        // 将用户Id 进行MD5加密，加密后的字符串，就是一个下单码。
        String userId = AuthContextHolder.getUserId(request);
        // 用户要秒杀的商品
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsById(skuId);
        // 判断
        if(null!=seckillGoods){
         // 获取下单码，在商品的秒杀时间范围内才能获取。在活动开始之后，结束之前。
            Date curTime = new Date(); // 获取当前系统时间
            if (DateUtil.dateCompare(seckillGoods.getStartTime(),curTime) && DateUtil.dateCompare(curTime,seckillGoods.getEndTime())){
                // 符合条件，才能生成下单码
                String skuIdStr = MD5.encrypt(userId);
                // 保存skuIdStr 返回给页面使用
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("获取下单码失败！");
    }

    // '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr
    @PostMapping("auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,HttpServletRequest request){
        // 检查下单码
        String skuIdStr = request.getParameter("skuIdStr"); // 页面提交过来的下单码
        // 下单码生成的规则：md5将用户Id，加密
        String userId = AuthContextHolder.getUserId(request);
        // 根据后台规则生成的下单码
        String skuIdStrRes = MD5.encrypt(userId);
        if (!skuIdStr.equals(skuIdStrRes)){
            // 请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 获取状态位
        String status = (String) CacheHelper.get(skuId.toString());
        // 判断状态位
        if (StringUtils.isEmpty(status)){
            // 请求不合法
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 可以抢购
        if ("1".equals(status)){
            // 记录当前谁在抢购商品 ，自定义抢购用户的实体类
            UserRecode userRecode = new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(skuId);

            // 需要将用户放入队列中进行排队。
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
        }else {
            // 说明商品已经售罄
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }

        return Result.ok();
    }

    // 轮询页面的状态
    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 调用服务层的方法
        return seckillGoodsService.checkOrder(skuId,userId);
    }
    // 准备给下订单页面提供数据支持。
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        // 显示收货人地址，送货清单，总金额等。
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取收货地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 获取用户购买的商品
        // key = seckill:orders field = userId value=用户秒杀得到的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode){
            return Result.fail().message("非法操作");
        }
        // 获取用户购买的商品
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        // 声明一个集合来存储订单明细
        List<OrderDetail> detailArrayList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        // 给订单明细赋值
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setSkuNum(orderRecode.getNum());// 在页面显示用户秒杀的商品 给orderRecode 的num
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        // 还需要将数据保存到数据库。
        detailArrayList.add(orderDetail);

        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        orderInfo.sumTotalAmount();

        // 声明一个map 集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("detailArrayList",detailArrayList);
        map.put("userAddressList",userAddressList);
        map.put("totalAmount",orderInfo.getTotalAmount());
        map.put("totalNum",orderRecode.getNum());
        return Result.ok(map);
    }

    // 提交订单
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        // 获取userId
        String userId = AuthContextHolder.getUserId(request);
        // 赋值用户Id
        orderInfo.setUserId(Long.parseLong(userId));
        // 获取用户购买的数据 key = seckill:orders field = userId value=用户秒杀得到的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode){
            return Result.fail().message("非法操作");
        }
        // 提交订单的操作
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (null==orderId){
            return Result.fail().message("非法操作,下单失败！");
        }

        // 删除下单信息
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);

        // 将用户真正的下单记录保存上
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId.toString());

        return Result.ok(orderId);
    }
}
