package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author mqx
 * @date 2020/5/4 16:27
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper,OrderInfo> implements OrderService {

    // 保存数据
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String WARE_URL; //WARE_URL=http://localhost:9001

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {

        // 保存orderInfo
        // 缺少部分数据 总金额，userId,订单状态，第三方交易编号，创建订单时间，订单过期时间，进程状态。
        orderInfo.sumTotalAmount();
        // userId 在控制器能获取到。暂时不用写。
        // 订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // 第三方交易编号 - 给支付宝使用的。能够保证支付的幂等性。
        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 创建时间
        orderInfo.setCreateTime(new Date());
        // 过期时间
        // 先获取日历对象
        Calendar calendar = Calendar.getInstance();
        // 在日历对象基础上添加一天
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 进程状态 与订单状态有个绑定关系。
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        // 订单的主题描述：获取订单明细中的商品名称，将商品名称拼接在一起。
        // 订单明细：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuilder sb=new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            sb.append(orderDetail.getSkuName()+" ");
        }
        // 做个长度处理
        if (sb.toString().length()>100){
            orderInfo.setTradeBody(sb.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(sb.toString());
        }
        orderInfoMapper.insert(orderInfo);


        for (OrderDetail orderDetail : orderDetailList) {
            // 赋值orderId
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }

        // 发送消息队列：发送延迟消息,如果在规定的时间内未付款，那么就取消订单。根据订单Id取消。
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,orderInfo.getId(),MqConst.DELAY_TIME);
        // 返回订单Id
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 获取流水号
        String tradeNo = UUID.randomUUID().toString().replace("-","");
        // 将流水号放入缓存
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 使用String 数据类型就可以。
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo ;
    }

    /**
     *
     * @param tradeNo 页面的流水号
     * @param userId 获取缓存的流水号
     * @return
     */
    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        // 获取缓存的流水号
        // 将流水号放入缓存
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 获取缓存的流水号
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void deleteTradeNo(String userId) {
        // 将流水号放入缓存
        String tradeNoKey = "user:"+userId+":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用库存系统 httpClientUtil 工具类远程调用。
        // http://localhost:9001/hasStock?skuId=22&num=1
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        // 为什么这个位置使用的是 HttpClient ，不是feign 远程调用。是因为仓库系统，是一个单独的spring boot 工程。
        // 0 表示没有库存，1表示有足够的库存
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        // 关闭订单 order_status=CLOSED process_status = CLOSED
        // update oder_info set order_status=CLOSED ,process_status = CLOSED where id = orderId
        // 后续还会做一些关于订单的更新操作，必然说：支付完成，order_status=PAID ,process_status = PAID
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        // 发送消息关闭支付宝订单,或者关闭交易记录。
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);

    }

    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        // update oder_info set order_status=CLOSED ,process_status = CLOSED where id = orderId
        // 声明一个OrderInfo 对象
        OrderInfo orderInfo = new OrderInfo();
        // 赋值更新条件
        orderInfo.setId(orderId);
        // 赋值更新的内容
        orderInfo.setProcessStatus(processStatus.name());
        // 订单状态，可以从进程状态中获取
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());

        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        // select * from order_info where id = orderId,
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        // select * from order_detali where order_id =orderIdorderId 还需要查询订单明细信息。
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(orderDetailQueryWrapper);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        // 更新订单状态
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        // 发送一个json 字符串。通过接口文档得知，发送的数据都是从OrderInfo 中获取的部分字段组成的json。
        String wareJson = initWareOrder(orderId);
        // 发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    // 获取发送 json 字符串
    public String initWareOrder(Long orderId) {
        // json 字符串是由OrderInfo 组成，所以我们必须先获取OrderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将 orderInfo 对象中的部分字段先转化为Map 集合，再转化为Json 字符串。
        Map map = initWareOrder(orderInfo);

        return JSON.toJSONString(map);
    }

    // 将 orderInfo 对象中的部分字段先转化为Map 集合
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        // details:[{skuId:101,skuNum:1,skuName:’小米手64G’},{skuId:201,skuNum:1,skuName:’索尼耳机’}]
        // 以上数据结构可以看成List<Map>
        List<Map> mapArrayList = new ArrayList<>();
        // 获取订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("skuId",orderDetail.getSkuId());
            hashMap.put("skuNum",orderDetail.getSkuNum());
            hashMap.put("skuName",orderDetail.getSkuName());
            mapArrayList.add(hashMap);
        }
        map.put("details",mapArrayList);
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(long orderId, String wareSkuMap) {
        /*
          1. 先获取原始订单，谁要被拆。
          2. 将wareSkuMap 参数转化为我们程序可以操作的对象。
          3. 创建一个新的子订单
          4. 给子订单进行赋值
          5. 保存子订单
          6. 将子订单添加到集合
          7. 修改订单的状态
         */

        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // wareSkuMap=[{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}] -- List<Map>
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        // 判断当前的map 集合是否为空，然后进行操作。
        if (null!=mapList && mapList.size()>0){
            for (Map map : mapList) {
                // 获取仓库Id
                String wareId = (String) map.get("wareId");
                // 获取skuId 集合
                List<String> skuIds = (List<String>) map.get("skuIds");
                OrderInfo subOrderInfo = new OrderInfo();
                // 通过属性拷贝。
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                // 拷贝的时候注意Id,主键自增
                subOrderInfo.setId(null);
                // 给原始订单Id
                subOrderInfo.setParentOrderId(orderId);
                // 赋值仓库Id
                subOrderInfo.setWareId(wareId);
                // 声明一个子订单明细
                List<OrderDetail> orderDetailLists = new ArrayList<>();
                // 赋值子订单的明细表。先获取原始订单明细集合
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    // 对比条件商品Id
                    for (String skuId : skuIds) {
                        // 判断skuId 是否存在。
                        if (Long.parseLong(skuId)==orderDetail.getSkuId().intValue()){
                            // 将子订单明细保存上。
                            orderDetailLists.add(orderDetail);
                        }
                    }
                }
                // 子订单明细赋值给子订单
                subOrderInfo.setOrderDetailList(orderDetailLists);
                // 计算子订单的总金额
                subOrderInfo.sumTotalAmount();
                // 保存子订单
                saveOrderInfo(subOrderInfo);
                // 添加子订单到集合
                subOrderInfoList.add(subOrderInfo);
            }
        }

        // 修改订单的状态。
        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {

        // 关闭订单 order_status=CLOSED process_status = CLOSED
        // update oder_info set order_status=CLOSED ,process_status = CLOSED where id = orderId
        // 后续还会做一些关于订单的更新操作，必然说：支付完成，order_status=PAID ,process_status = PAID

        // flag =1 的时候，只关闭orderInfo.
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)){
            // 发送消息关闭支付宝订单,或者关闭交易记录。
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }

    }


}
