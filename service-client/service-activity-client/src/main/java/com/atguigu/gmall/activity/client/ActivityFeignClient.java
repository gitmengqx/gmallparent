package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/5/11 11:27
 */
@FeignClient(name = "service-activity",fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient  {

    // 以下所有的远程调用：SeckillGoodsController 中对应的地址
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();


    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);

    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String, Object>> trade();
}
