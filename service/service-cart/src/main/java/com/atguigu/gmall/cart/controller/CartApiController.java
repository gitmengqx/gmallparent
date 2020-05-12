package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author mqx
 * @date 2020/4/30 11:45
 */
@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    // skuId=26&skuNum=1
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){

        // 如何获取用户Id http://cart.gmall.com/addCart.html?skuId=26&skuNum=1
        // 要走网关，在网关中设置了获取登录的用户Id，未登录的临时用户Id
        // 在common-util 中有个工具类AuthContextHolder
        // 表示获取登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            // 未登录获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 添加购物车方法
        cartService.addToCart(skuId,userId,skuNum);
        // 返回添加成功！
        return Result.ok();
    }
    // 获取购物车列表
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){

        // 获取用户Id -- 登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取临时用户Id
        String userTempId = AuthContextHolder.getUserTempId(request);
        // 获取返回的购物车集合
        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);
        // 将集合放入result
        return  Result.ok(cartList);
    }
    // /checkCart/' + skuId + '/' + isChecked
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        // 获取用户Id | 跟单点登录结合在一起。
        String userId = AuthContextHolder.getUserId(request);
        // 表示未登录
        if (StringUtils.isEmpty(userId)){
            // 获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用服务层方法
        cartService.checkCart(userId,isChecked,skuId);
        return Result.ok();
    }
    // 删除购物车
    // url: this.api_name + '/deleteCart/' + skuId,
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){

        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            // 获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用删除方法
        cartService.deleteCart(skuId,userId);
        return Result.ok();
    }

    // 从购物车中获取送货清单
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }
}
