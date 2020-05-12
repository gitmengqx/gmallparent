package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SkuSaleAttrValue;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author mqx
 * @date 2020/4/21 14:06
 */
@Service
public class ItemServiceImpl implements ItemService {
    // 在这个实现类中，如何给map 赋值。
    // 通过feign 远程调用service-product 中的方法。
    @Autowired
    private ProductFeignClient productFeignClient;

    // 从spring 容器获取线程池
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;


    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        Map<String, Object> map = new HashMap<>();
        // 调用通过skuId 查询skuInfo 数据
        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            map.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        // 保存销售属性，销售属性值集合到map中即可！
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 销售属性，销售属性值的数据集合
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            // 保存起来
            map.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);

        // 查询价格：
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            // 保存数据
            map.put("price", skuPrice);
        }, threadPoolExecutor);

        // 获取分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            // 保存数据
            map.put("categoryView", categoryView);
        }, threadPoolExecutor);

        // 通过 spuId 查询用户点击销售属性值时组成的sku
        CompletableFuture<Void> valuesSkuJsonCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            // 转化为json 字符串
            String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
            map.put("valuesSkuJson", valuesSkuJson);
        }, threadPoolExecutor);

        // 当商品详情页面被访问到的时候，那么应该调用一次热度排名。
        CompletableFuture<Void> incrHotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);


//        Map<String, Object> map = new HashMap<>();
//        // 调用通过skuId 查询skuInfo 数据
//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//
//        // 显示销售属性，销售属性值
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//
//        // 通过skuId 查询商品的价格
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//
//        // 通过三级分类Id 查询分类数据
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//
//        // 通过 spuId 查询用户点击销售属性值时组成的sku
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//
//        // 将map 保存起来，给前台页面提供数据 skuValueIdsMap 变成json 字符串
//        // map ---> json
//        String valuesSkuJson = JSON.toJSONString(skuValueIdsMap);
//        // 将集合数据放入map 中。map 放入数据的时候，key 是有一定的规则的，当前key存储的数据要与页面的获取的数据一致。
//        // index.html 后台存储数据，前台需要通过Thymeleaf ${skuInfo.getSkuName()} 页面中的数据绑定都封装好了。
//        // 页面封装的价格 ${price} 后台需要存储一个price 为key 的值。
//        map.put("skuInfo",skuInfo);
//        map.put("price",skuPrice);
//        map.put("categoryView",categoryView);
//        map.put("spuSaleAttrList",spuSaleAttrList);
//        // 保存json 字符串数据
//        map.put("valuesSkuJson",valuesSkuJson);
        // 返回map
        CompletableFuture.allOf(skuCompletableFuture,skuPriceCompletableFuture,
                categoryViewCompletableFuture,valuesSkuJsonCompletableFuture,
                spuSaleAttrCompletableFuture,incrHotScoreCompletableFuture).join();
        return map;
    }
}
