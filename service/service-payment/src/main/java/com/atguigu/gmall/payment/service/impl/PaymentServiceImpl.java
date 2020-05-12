package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sun.corba.se.spi.ior.iiop.IIOPFactories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/5/8 9:05
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    // 实现类通常调用mapper 层。
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    // 引入rabbitService
    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {

        // paymentInfo 数据记录的是当前一个订单的支付状态？
        // 一个订单，是不是应该只有一个交易记录。不能出现这种情况： 当前一个订单，即有支付宝交易记录，也有微信交易记录。
        // 还不能出现： 当前一个订单，有支付宝支付的两条交易记录。
        // no1: 94 ALIPAY ATGUIGU1586940873810151 0.01
        // no2: 94 ALIPAY ATGUIGU1586940873810151 0.01
        // 在paymentInfo 中应该允许存在。
        // 支付保中有幂等性：来确保多个人支付的时候，只能有一个人支付成功！ | 无论当前支付多少次，只能有一次支付成功。
        // 通过 out_trade_no 保证！ 	商户订单号,64个字符以内、可包含字母、数字、下划线；需保证在商户端不重复！
        // 如果一个订单下了之后，那么这个 out_trade_no 是唯一的，在支付宝中就记录了。
        // 目前：只有一种支付方式：ALIPAY
        // 查询交易记录，如果有支付类型，订单Id，是多条记录的那么这样的数据不应该存在。
        Integer count = paymentInfoMapper.selectCount(new QueryWrapper<PaymentInfo>().eq("order_id", orderInfo.getId()).eq("payment_type", paymentType));
        // 如果交易记录中已经存在了订单Id，和支付类型那么就不能再次插入同样的数据了。
        if (count>0) return;

        // 创建一个paymentInfo 对象。
        PaymentInfo paymentInfo = new PaymentInfo();
        // 给 paymentInfo 赋值。 数据来源orderInfo 中。
        // 查询 orderInfo 数据。{orderId,outTradeNo,subject,totalAmount...}根据订单Id 查询数据。
        Long orderId = orderInfo.getId();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setPaymentType(paymentType);// 支付类型。
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        // select * from payment_info where out_trade_no=out_trade_no and payment_type=name
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    @Override
    public void paySuccess(String outTradeNo, String name) {
        // update payment_info set payment_status=PAID,callback_time=new Date() where out_trade_no=outTradeNo and payment_type=name;

        // 第一个参数paymentInfo ， 表示更新的内容放入paymentInfo 中。
        // 第二个参数更新条件，
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        // 更新payment_status=PAID
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        // 更新回调时间
        paymentInfoUpd.setCallbackTime(new Date());
        // 更新回调内容 callback_content 可以随意写。
        paymentInfoUpd.setCallbackContent("异步回调啦！");
        // 构建更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        paymentInfoMapper.update(paymentInfoUpd,paymentInfoQueryWrapper);

    }
    // trade_no,payment_status,callback_time,callback_content
    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        // update payment_info set payment_status=PAID,callback_time=new Date() where out_trade_no=outTradeNo and payment_type=name;
        // 第一个参数paymentInfo ， 表示更新的内容放入paymentInfo 中。
        // 第二个参数更新条件，
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        // 更新payment_status=PAID
        paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID.name());
        // 更新回调时间
        paymentInfoUpd.setCallbackTime(new Date());
        // 更新回调内容 callback_content 可以随意写。
        paymentInfoUpd.setCallbackContent(paramMap.toString());

        // 追加更新trade_no
        // trade_no 支付宝交易号。在paramMap中可以获取。
        String trade_no = paramMap.get("trade_no");
        paymentInfoUpd.setTradeNo(trade_no);

        // 构建更新条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo).eq("payment_type",name);
        paymentInfoMapper.update(paymentInfoUpd,paymentInfoQueryWrapper);

        // 如果没有订单Id 那么就查询 getPaymentInfo();
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, name);
        // 支付成功之后，发送消息通知订单。 更改订单状态。
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    }

    @Override
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        // update payment_info set payment_status=PAID where out_trade_no=outTradeNo and payment_type=name;
        paymentInfoMapper.update(paymentInfo,new QueryWrapper<PaymentInfo>().eq("out_trade_no",outTradeNo));

    }

    @Override
    public void closePayment(Long orderId) {

        // 更新paymentInfo payment_status=CLOSED
        // 第一个参数表示更新内容，第二个参数表示更新条件
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());

        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderId);
        // update payment_info set payment_status=CLOSED where order_id = ?
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        // 如果交易记录中没有当前数据，则返回，不执行关闭。
         /*
        交易记录表中的数据什么时候才会产生？
        当用户点击支付宝生成支付二维码的时候，paymentInfo 才会有记录。
        如果只是下单，不点生成二维码的时候，这个表是没有数据的。
        根据上述条件，先查询是否有交易记录，如果没有交易记录，则不关闭。
         */
        if (null == count || count.intValue()==0){
            return;
        }
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }
}
