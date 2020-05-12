package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/5/11 11:31
 */
@Controller
public class SeckilController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @GetMapping("seckill.html")
    public String getAll(Model model){
        // 获取秒杀商品数据
        Result result = activityFeignClient.findAll();

        // 后台应该存储一个list 集合数据
        model.addAttribute("list",result.getData());
        // 返回秒商品页面
        return "seckill/index";
    }

    @GetMapping("seckill/{skuId}.html")
    public String getItme(@PathVariable Long skuId,Model model){
        // 根据skuId 获取秒杀详情数据
        Result result = activityFeignClient.getSeckillGoods(skuId);
        // 应该存储一个item 的对象
        model.addAttribute("item",result.getData());
        // 返回商品详情页面
        return "seckill/item";
    }
    // 根据请求要获取skuId,skuIdStr '/seckill/queue.html?skuId='+this.skuId+'&skuIdStr='+skuIdStr
    // springmvc 讲的。
    @GetMapping("seckill/queue.html")
    public String queue(@RequestParam(name = "skuId") Long skuId,
                        @RequestParam(name = "skuIdStr") String skuIdStr,
                        HttpServletRequest request){

        // 存储skuIdStr，skuId
        request.setAttribute("skuId",skuId);
        request.setAttribute("skuIdStr",skuIdStr);
        return "seckill/queue";
    }

    @GetMapping("seckill/trade.html")
    public String trade(Model model){
        // 获取到下单数据
        Result<Map<String,Object>> result = activityFeignClient.trade();

        if (result.isOk()){
            // 将数据保存，给页面提供渲染
            model.addAllAttributes(result.getData());
            // 返回订单页面
            return "seckill/trade";
        }else {
            // 存储失败信息
            model.addAttribute("message",result.getMessage());
            // 返回订单页面
            return "seckill/fail";
        }

    }

}
