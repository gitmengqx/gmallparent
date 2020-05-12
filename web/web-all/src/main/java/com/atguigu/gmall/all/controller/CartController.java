package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/5/4 10:27
 */
@Controller
public class CartController {

    // 引入cartFeignCliet
    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    // http://cart.gmall.com/addCart.html?skuId=23&skuNum=1
    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          HttpServletRequest request){
        // String skuId = request.getParameter("skuId");
        // String userId = AuthContextHolder.getUserId(request);
        cartFeignClient.addToCart(skuId,skuNum);
        // 存储skuInfo,skuNum
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        // 返回添加成功页面
        return "cart/addCart";
    }

    @RequestMapping("cart.html")
    public String cart(){
        return "cart/index";
    }
}
