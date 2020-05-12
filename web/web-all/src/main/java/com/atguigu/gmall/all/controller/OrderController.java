package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/29 15:42
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    // http://order.gmall.com/trade.html
    @GetMapping("trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result = orderFeignClient.trade();
        model.addAllAttributes(result.getData());
        // 保存数据
        // 返回页面
        return "order/trade";
    }
}
