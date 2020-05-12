package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author mqx
 * 取消订单
 * @date 2020/5/6 14:27
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private RabbitService rabbitService;

    // 获取队列中的消息
    // 发送消息的时候，发送的是订单Id，
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel){
        // 判断订单Id 是否为空
        if(null!=orderId){
            // 根据订单Id 查询订单表中是否有当前记录
            OrderInfo orderInfo = orderService.getById(orderId);
            if (null!=orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                // 先关闭paymentInfo 后关闭orderInfo,因为 支付成功之后，异步回调先修改的paymentInfo,然后在发送的异步通知修改订单的状态。
                // 关闭流程，应该先看电商平台的交易记录中是否有数据，如果有则关闭。
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                // 判断电商交易记录 ,交易记录表中有数据，那么用户一定走到了二维码那一步。
                if (null!=paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    // 检查支付宝中是否有交易记录
                    Boolean flag = paymentFeignClient.checkPayment(orderId);
                    // 说明用户在支付宝中产生了交易记录，用户是扫了。
                    if (flag){
                        // 关闭支付宝
                        Boolean result = paymentFeignClient.closePay(orderId);
                        // 判断是否关闭成功
                        if (result){
                            // 关闭支付宝的订单成功 关闭 OrderInfo 表,paymentInfo
                            orderService.execExpiredOrder(orderId,"2");
                        }else {
                            // 关闭支付宝的订单失败，如果用户付款成功了，那么我们调用关闭接口是失败！
                            // 如果成功走正常流程
                            // 很极端，测试。。。。。
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,orderId);
                        }
                    }else {
                        // 说明用户根本没有扫描，说明到了二维码
                        // 关闭支付宝的订单成功 关闭 OrderInfo 表,paymentInfo
                        orderService.execExpiredOrder(orderId,"2");
                    }
                }else {
                    // 说明paymentInfo 中根本就没有数据 ，没有数据，那么就只需要关闭orderInfo,
                    orderService.execExpiredOrder(orderId,"1");
                }
            }
        }
//            // 手动确认消息已经处理了。
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
    // 监听消息，然后更改订单的状态。
    // rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void getMsg(Long orderId,Message message,Channel channel){
        // 判断orderId 不能为空
        if (null!=orderId){
            // 判断支付状态是未付款
            OrderInfo orderInfo = orderService.getById(orderId);
            if (null!=orderId){
                if (orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                    // 更新订单的状态。
                    orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                    // 发送消息给库存。
                    orderService.sendOrderStatus(orderId);
                }
            }
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    // 监听库存系统减库存的消息队列。
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson,Message message,Channel channel){
        // 判断发送的消息不能为空。
        if (!StringUtils.isEmpty(msgJson)){
            // msgJson 是有map 组成的。将这个字符串在转为map，获取里面的orderId,status
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");

            // 判断减库存是否成功。
            if("DEDUCTED".equals(status)){
                // 减库存成功，将订单的状态等待发货。
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
            }else {
                // 减库存失败，订单中的商品数量>库存数量 此时会发生超卖。
                // 1.   调用其他仓库的库存，补货。   2.  人工客服介入与客户进行沟通。
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        }
    }
}
