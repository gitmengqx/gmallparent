package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

import static com.alipay.api.AlipayConstants.SIGN_TYPE;
import static org.springframework.data.redis.core.convert.Bucket.CHARSET;

/**
 * @author mqx
 * @date 2020/5/8 10:39
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    // 根据订单Id 来完成支付二维码显示
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable Long orderId){
        // 调用方法
        String aliPay= "";
        try {
            aliPay = alipayService.createAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return aliPay;
    }

    // 找到同步回调地址：return_payment_url=http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("callback/return")
    public String callBack(){
        // 给支付成功页面。
        // return_order_url=http://payment.gmall.com/pay/success.html
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    // 异步回调： 它需要做内网穿透，异步回调需要支付宝与电商平台做数据校验。
    // 校验过程： https://opendocs.alipay.com/open/270/105902
    // notify_payment_url=http://t5msem.natappfree.cc/api/payment/alipay/callback/notify
    @RequestMapping("callback/notify")
    @ResponseBody
    public String aliPayNotify(@RequestParam Map<String ,String> paramMap){
        //Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false;

        // 因为将支付宝异步通知的参数封装到paramMap集合中
        String trade_status = paramMap.get("trade_status");
        // 获取out_trade_no 查询当前的paymentInfo 数据。
        String out_trade_no = paramMap.get("out_trade_no");
        try {
            // 验证签名成功。
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 通过验证 totalAmout 等参数。
            // 在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            // TRADE_SUCCESS||TRADE_FINISHED 表示在支付宝中已经支付支付了。交易记录 状态应该是 PAID
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 支付成功了，需要更改状态支付状态  payment_status = PAID
                /*
                    以下出现的概率很小，
                 用户下单之后，没有直接付款，订单会有一个过期时间{10分钟}。
                 等到9'58''开始付款。10分钟到了取消订单线程，
                 关闭过期订单：需要看订单的状态是否付款{关闭过期订单，需要查看paymentInfo,如果说交易记录状态nupaid，正常关闭。
                 如果说付款，不能关闭了!}。

                 异步回调的时候，返回成功，或者是失败！ 判断当前的交易记录状态，不能为PAID或者是CLOSED.

                 等到用户超过10分钟付款。那么你说这个异步回调。应该失败！ CLOSED关闭，提示用户异步支付也是失败！
                 */
                // 查询支付交易记录状态， 如果是 payment_status=PAID,CLOSED 那么应该返回failure。
                // 一个订单只能有一次支付成功！ 目的：防止用户重复付款！
                // 只有当前交易记录为UNPAID ，他在支付的时候已经成功！
                // 查询支付状态
                // 返回支付成功！

                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());

                // out_trade_no,金额，等数据。
                // 查询支付交易记录状态， 如果是 payment_status=PAID,CLOSED 那么应该返回failure。
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())){
                    return "failure";
                }
//                String total_amount = paramMap.get("total_amount");
                // 验证金额，outTradeNo,app_id 都要获取全部通过才能返回success.
//                int amount = Integer.parseInt(total_amount);
//                BigDecimal totalAmount = new BigDecimal(total_amount);
//                if (paymentInfo.getTotalAmount().compareTo(totalAmount)==0 && paymentInfo.getOutTradeNo().equals(out_trade_no)){
//                    // 处理PAID,CLOSED 之外，那么就应该更新交易记录。
//                    paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramMap);
//                    // 返回支付成功
//                    return "success";
//                }
                // 处理PAID,CLOSED 之外，那么就应该更新交易记录。
                paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramMap);
                // 返回支付成功
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
        // 调用退款接口
        boolean flag =  alipayService.refund(orderId);
        return Result.ok(flag);
    }

    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody // 将返回来的数据直接显示到页面。
    public Boolean checkPayment(@PathVariable Long orderId){
        // 检查交易记录
        Boolean aBoolean = alipayService.checkPayment(orderId);
        return aBoolean;
    }

    // 根据订单Id关闭订单
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        Boolean aBoolean = alipayService.closePay(orderId);
        return aBoolean;
    }

    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null!=paymentInfo){
            return paymentInfo;
        }
        return null;
    }
}
