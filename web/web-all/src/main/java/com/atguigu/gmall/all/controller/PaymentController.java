package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/5/6 16:16
 */
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    // http://payment.gmall.com/pay.html?orderId=92
    @GetMapping("pay.html")
    public String pay(HttpServletRequest request){
        // 获取订单Id
        String orderId = request.getParameter("orderId");
        // 远程调用订单
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        // 保存作用域
        request.setAttribute("orderInfo",orderInfo);
        // 返回支付页面
        return "payment/pay";
    }

    // http://payment.gmall.com/pay/success.html
    @GetMapping("pay/success.html")
    public String success(){
        return "payment/success";
    }
}
