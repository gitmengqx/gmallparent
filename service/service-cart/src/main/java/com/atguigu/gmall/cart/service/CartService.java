package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author mqx
 * @date 2020/4/30 11:15
 */
public interface CartService {
    // 添加购物车抽象方法
    // 添加购物车 用户Id，商品Id，商品数量。
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 根据用户Id 查询购物车列表
     * @param userId 登录的用户Id
     * @param userTempId 未登录的用户Id
     * @return
     */
    List<CartInfo> getCartList(String userId,String userTempId);

    /**
     * 购物车选中状态
     * @param userId 用户Id
     * @param isChecked 选中状态
     * @param skuId 用户Id
     */
    void checkCart(String userId,Integer isChecked,Long skuId);

    /**
     * 删除购物车数据
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据用户Id 查询购物车列表{被选中的商品组成送货清单}
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户Id 加载购物车数据，并放入缓存。
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
