package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author mqx
 * @date 2020/4/22 10:33
 */
@FeignClient(value = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {
    // 调用service-item 中的方法
    // 通过skuId 查询商品详情数据
    // 用户调用ItemFeignClient.getItem() 这个方法的时候，本质是调用ItemApiController.getItem();
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable Long skuId);

}
