package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/21 11:56
 */
public interface ItemService {
    // 商品详情页面要想获取到数据，那么必须有一个skuId !
    // 那么skuId 在哪？如何传递到商品详情页面的？
    // 商品详情页面是从list.html 检索页面过来的。
    // https://item.jd.com/100005207363.html
    // https://item.jd.com 域名
    // 100005207363.html 控制器，并不是一个单纯的html
    // {skuId}.html

    /**
     * 我需要将数据封装到map中。
     * map.put("price","商品的价格")
     * map.put("skuInfo","skuInfo数据")
     * @param skuId 库存单元的Id
     * @return
     */
    Map<String,Object> getBySkuId(Long skuId);



}
