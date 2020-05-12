package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/5/4 14:50
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;
    
    @Autowired
    private OrderService orderService;

    // auth/trade 用户走这个控制器,那么必须登录?
    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户的地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        // 获取送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        // 声明一个送货清单的集合
        List<OrderDetail> orderDetailList = new ArrayList<>();

        int totalNum = 0;
        // 送货清单应该是OrderDetail
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());

            // 记录件数+ 让每个商品的skuNum 相加即可！
            totalNum+=cartInfo.getSkuNum();
            // 将每个订单明细添加到当前的集合中
            orderDetailList.add(orderDetail);
        }
        // 算出当前订单的总金额。
        OrderInfo orderInfo = new OrderInfo();
        // 将订单明细赋值给orderInfo
        orderInfo.setOrderDetailList(orderDetailList);
        // 计算总金额
        orderInfo.sumTotalAmount();

        // 将数据封装到map集合中。
        HashMap<String, Object> map = new HashMap<>();
        // 保存总金额。通过页面trade.html 可以找到页面对应存储的key{totalAmount}
        map.put("totalAmount",orderInfo.getTotalAmount());
        // 保存userAddressList
        map.put("userAddressList",userAddressList);
        // 保存totalNum
        // 那集合长度跟商品数就不一定相等了啊,
//        map.put("totalNum",orderDetailList.size()); 以spu为总数
        // 以sku的件数为总数detailArrayList
        map.put("totalNum",totalNum);
        // 保存detailArrayList
        map.put("detailArrayList",orderDetailList);

        // 生成一个流水号，并且保存到作用域，给页面使用。
        String tradeNo = orderService.getTradeNo(userId);
        // 保存tradeNo
        map.put("tradeNo",tradeNo);
        // 返回数据集合
        return Result.ok(map);
    }

    // 下订单的控制器 带有 auth 用户必须登录
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        // userId 在控制器能获取到。暂时不用写。
        String userId = AuthContextHolder.getUserId(request);
        // 在保存之前将用户Id赋值给orderInfo
        orderInfo.setUserId(Long.parseLong(userId));

        // 下订单之前做校验：流水号不能无刷新重复提交。
        String tradeNo = request.getParameter("tradeNo");
        // 调用比较方法即可。
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
        // 判断比较结果
        if (!flag){
            // 提示不能下单了。
            return Result.fail().message("不能无刷新回退重复下订单！");
        }
        // 删除流水号
        orderService.deleteTradeNo(userId);

        // 验证库存： 用户购买的每个商品都必须验证：
        // 循环订单明细中的每个商品。
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (null!=orderDetailList && orderDetailList.size()>0){
            for (OrderDetail orderDetail : orderDetailList) {
                // 循环判断 result=true 表示有足够的库存，如果result=fasle 表示没有足够的库存。
                boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
                if (!result){
                    // 没有足够的库存
                    return Result.fail().message(orderDetail.getSkuName()+"没有足够的库存！");
                }
                // 检查价格是否有变动。 orderDetail.getOrderPrice() == skuPrice;
                // 如果比较结果不一致： 价格有变动，提示用户重新下订单。购物车的价格也需要变化。
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                if (orderDetail.getOrderPrice().compareTo(skuPrice)!=0){
                    // 判断只要不等于0 那么就说明价格有变动。
                    // 更新购物车中的价格： 从新查询一遍
                    cartFeignClient.loadCartCache(userId);

                    return Result.fail().message(orderDetail.getSkuName()+"商品价格有变动，请重新下单！");
                }
            }
        }

        Long orderId = orderService.saveOrderInfo(orderInfo);
        // 返回数据
        return Result.ok(orderId);
    }

    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){
        // 调用方法
        return orderService.getOrderInfo(orderId);
    }
    // http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        // 获取传递过来的参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 获取子订单集合,根据当前传递过来的参数进行获取。
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);
        List<Map> mapArrayList = new ArrayList<>();
        // 获取子订单集合的字符串。
        for (OrderInfo orderInfo : subOrderInfoList) {
            // 将子订单中的部分数据变成map，再将map 转化为字符串。
            // 一个map 集合表示一个子订单对象，因为拆单可能有多个orderInfo ,所有将map 放入一个集合中统一存储。
            Map map = orderService.initWareOrder(orderInfo);
            mapArrayList.add(map);
        }
        // 返回子订单的集合字符串
        return JSON.toJSONString(mapArrayList);
    }

    // 提交秒杀订单
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){

        // 保存订单数据
        Long orderId = orderService.saveOrderInfo(orderInfo);

        return orderId;
    }
}
