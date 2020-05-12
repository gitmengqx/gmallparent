package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author mqx
 * 支付接口
 * @date 2020/5/8 9:01
 */
public interface PaymentService {
    // 保存交易记录
    // 参数的确定？

    /**
     *
     * @param orderInfo 保存交易记录的数据
     * @param paymentType 支付方式
     */
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);
    // 根据outTradeNo,付款方式查询交易记录
    PaymentInfo getPaymentInfo(String outTradeNo, String name);
    // 根据outTradeNo,付款方式更新交易记录。callback_time,payment_status,
    // trade_no更新不了的。trade_no 表示支付宝的交易号。支付宝的交易号，在回调的参数中。
    void paySuccess(String outTradeNo, String name);
    // 根据outTradeNo,付款方式更新交易记录,trade_no,payment_status,callback_time,callback_content

    /**
     *
     * @param outTradeNo
     * @param name
     * @param paramMap 支付宝回调的参数的map
     */
    void paySuccess(String outTradeNo, String name, Map<String, String> paramMap);

    /**
     * 更新方法
     * @param outTradeNo
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    /**
     * 关闭交易记录
     * @param orderId
     */
    void closePayment(Long orderId);
}
