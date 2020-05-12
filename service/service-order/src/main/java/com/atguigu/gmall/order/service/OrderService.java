package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/5/4 16:24
 */
public interface OrderService extends IService<OrderInfo> {

    // 定义保存订单 由trade.html 可知需要返回一个订单Id的
    // 传入的参数：通过trade.html
    Long saveOrderInfo(OrderInfo orderInfo);

    // 生成流水号，同时放入缓存。
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param tradeNo 页面的流水号
     * @param userId 获取缓存的流水号
     * @return
     */
    boolean checkTradeNo(String tradeNo,String userId);

    /**
     * 删除缓存的流水号
     * @param userId
     */
    void deleteTradeNo(String userId);

    /**
     * 根据skuId,skuNum 判断是否有足够的库存。
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 根据orderId 关闭过期订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据orderId 修改订单的状态。
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据orderId 查询订单数据
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 通过orderId 发送消息给库存，通知减库存。
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo 变为map集合。
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单方法。
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(long orderId, String wareSkuMap);

    /**
     * 关闭过期订单
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId, String flag);
}
