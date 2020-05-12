package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/22 10:39
 */
@Controller // 千万不要写@RestController
public class ItemController {

    // 通过feign 远程调用service-item
    @Autowired
    private ItemFeignClient itemFeignClient;
    // 用户是如何访问到商品详情？
    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
        // 通过feign 远程调用获取商品详情数据
        // 用户调用ItemFeignClient.getItem() 这个方法的时候，本质是调用ItemApiController.getItem();
        Result<Map> result = itemFeignClient.getItem(skuId);

        // 在后台应该将商品详情页面的数据保存到后台。
        // addAllAttributes 使用这个方法的原因是：因为map 中存储了多个key，value 所有采用该方法！
        model.addAllAttributes(result.getData());
        // 返回商品详情页面！
        return "item/index";
    }



}
