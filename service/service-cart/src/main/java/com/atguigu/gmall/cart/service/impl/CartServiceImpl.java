package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author mqx
 * @date 2020/4/30 11:16
 */
@Service
public class CartServiceImpl implements CartService {

    // 引入mapper
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        /*
        添加购物车判断购物车中是否有该商品
        true: 数量相加
        false: 直接添加

        特殊处理：添加购物车的时候，直接将购物车添加到缓存中。
         */
        // 获取购物车的key
        String cartKey = getCartKey(userId);
        // 判断缓存中是否有购物车的key！
        if (!redisTemplate.hasKey(cartKey)){
            // 查询数据库并添加到缓存。
            loadCartCache(userId);
        }

        // select * from cart_info where sku_id=? and user_id=?
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("sku_id",skuId).eq("user_id",userId);
        // 看数据库中购物车是否有添加的商品
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQueryWrapper);
        // 判断
        if (null!=cartInfoExist){
            // 说明购物车中已经添加过当前商品
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 赋值一个实时价格，在数据库中不存在的。
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            cartInfoExist.setSkuPrice(skuPrice);

            // 更新数据库
            cartInfoMapper.updateById(cartInfoExist);
            // 添加到缓存：添加完成之后，如果查询购物车列表的时候，直接走缓存了。如果缓存过期了，才走数据库。
            // redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        }else{
            // 说明购物车中没有添加的商品。
            CartInfo cartInfo = new CartInfo();
            // 给当前的cartInfo 赋值。
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            // 第一次添加购物车的时候，添加购物车的价格与实时价格应该是统一的！
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            // cartInfo.setIsChecked();// 默认是选中状态
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfoMapper.insert(cartInfo);

            // 添加到缓存：添加完成之后，如果查询购物车列表的时候，直接走缓存了。如果缓存过期了，才走数据库。
            // redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfo);
            cartInfoExist = cartInfo;
        }
        // 添加到缓存：添加完成之后，如果查询购物车列表的时候，直接走缓存了。如果缓存过期了，才走数据库。
        // 使用hash 数据类型 hset(key,field,value) key=user:userId:cart field=skuId value=购物车的字符串。
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        // 购物车在缓存中应该有过期时间
        setCartKeyExpire(cartKey);
    }

    /**
     *
     * @param userId 登录的用户Id
     * @param userTempId 未登录的用户Id
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        // 声明一个集合来存储购物车数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 判断用户是否登录 , 未登录
        if (StringUtils.isEmpty(userId)){
            // 获取临时购物车数据
            cartInfoList = getCartList(userTempId);
            return cartInfoList;
        }
        // 登录
        if (!StringUtils.isEmpty(userId)){
            // 1.   查询未登录购物车中是否有数据
            List<CartInfo> cartTempList = getCartList(userTempId);
            // 2.   当临时购物车数据不为空的情况下
            if(!CollectionUtils.isEmpty(cartTempList)){
                // 登录+未登录 合并之后的数据
                cartInfoList =  mergeToCartList(cartTempList,userId);
                // 3.   删除未登录购物车中的数据
                deleteCartList(userTempId);
            }
            // 如果未登录购物车没有数据
            if (CollectionUtils.isEmpty(cartTempList) || StringUtils.isEmpty(userTempId)){
                // 获取登录购物车数据
                cartInfoList = getCartList(userId);
            }
            return cartInfoList;
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        // 思路：需要，更改当前是商品 中ischeced ，
        // 必须知道商品存在什么位置 ，第一个 mysql，第二个 redis。
        // update cart_info set is_checked=isChecked where user_id=userId and sku_id=skuId;
        // redis --- 先将redis 中的数据查询出来，cartInfo. cartInfo.setIsChecked(isChecked);

        // 第一个参数CartInfo , 表示更新的内容。
        // 第二个参数更新条件
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,cartInfoQueryWrapper);

        // 获取缓存的key=user:userId:cart
        String cartKey = getCartKey(userId);
        // 根据hash 数据结构来获取数据
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        // 判断选中的商品在购物车中是否存在。
        if (boundHashOperations.hasKey(skuId.toString())){
            // 获取当前的商品Id 对应的cartInfo
            CartInfo cartInfoUpd = (CartInfo) boundHashOperations.get(skuId.toString());
            // 赋值选中状态
            cartInfoUpd.setIsChecked(isChecked);
            // 将改后的cartInfo 放入缓存
            boundHashOperations.put(skuId.toString(),cartInfoUpd);
            // 每次修改缓存完成之后，需要设置过期时间
            setCartKeyExpire(cartKey);
        }
    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        // 删除两个地方，一个是mysql，一个是redis
        // 获取缓存的key 最好，先删除缓存，后删除数据库。
        // 如果有mysql ，redis 这个地方数据尽量应该保持一致。先删除缓存，在操作数据库。
        String cartKey = getCartKey(userId);

        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        // 判断缓存中是否有商品Id
        if (boundHashOperations.hasKey(skuId.toString())){
            // 如果有这个key 则删除
            boundHashOperations.delete(skuId.toString());
        }

        // 删除数据库
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userId).eq("sku_id",skuId));


    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        // 在展示购物车列表中才能点击去结算,做订单页面.
        // 直接查询缓存即可!
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 定义缓存的key
        String cartKey = getCartKey(userId);
        // 获取缓存中的数据
        List<CartInfo> cartCacheList = redisTemplate.opsForHash().values(cartKey);
        if (null!=cartCacheList && cartCacheList.size()>0){
            // 循环遍历购物车中的数据
            for (CartInfo cartInfo : cartCacheList) {
                // 获取被选中的数据
                if (cartInfo.getIsChecked().intValue()==1){
                    // 将被选中的商品添加到集合中.
                    cartInfoList.add(cartInfo);
                }
            }
        }
        return cartInfoList;
    }

    //  删除购物车
    private void deleteCartList(String userTempId) {
        // 未登录购物车数据：一个是存在缓存，一个是存在数据库。
        // 删除数据，对数据进行 DML 操作 DML:insert,update,delete
        // 先删缓存 先获取 购物车的key
        String cartKey = getCartKey(userTempId);
        Boolean aBoolean = redisTemplate.hasKey(cartKey);
        // 如果缓存有key
        if (aBoolean){
            // 则删除。
            redisTemplate.delete(cartKey);
        }
        // 删除数据库
        // delete from cart_info where user_id = userTempId;
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userTempId));

    }

    // 合并购物车
    private List<CartInfo> mergeToCartList(List<CartInfo> cartTempList, String userId) {
        // 合并登录+未登录
        // 通过用户Id 获取登录数据
        List<CartInfo> cartLoginList = getCartList(userId);
        // 合并条件是什么? skuId
        /*
        demo1:
            登录：
                37 1
                38 1
            未登录：
                37 1
                38 1
                39 1
            合并之后的数据
                37 2
                38 2
                39 1
         demo2:
             未登录：
                37 1
                38 1
                39 1
                40 1
              合并之后的数据
                37 1
                38 1
                39 1
                40 1
         */
        // 表示以skuId为key ，以 cartInfo 为value 一个map集合。
        Map<Long, CartInfo> cartInfoMapLogin = cartLoginList.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        // 循环未登录购物车数据
        for (CartInfo cartInfoNoLogin : cartTempList) {
            // 取出未登录的商品Id
            Long skuId = cartInfoNoLogin.getSkuId();
            // 看登录购物车中是否有未登录的skuId
            if (cartInfoMapLogin.containsKey(skuId)){ // 有
                // 获取登录数据
                CartInfo cartInfoLogin = cartInfoMapLogin.get(skuId);
                // 商品数量相加
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());

                // 细节操作：合并的时候，需要判断是否有商品被勾选
                // 未登录状态下，有商品是选中状态！
                if (cartInfoNoLogin.getIsChecked().intValue()==1){
                    // 那么登录状态下的商品应该为选中。
                    cartInfoLogin.setIsChecked(1);
                }
                // 更新数据库 37 1 ，38 1 放入数据库。
                cartInfoMapper.updateById(cartInfoLogin);
            }else { // 没有
                // 将输入直接插入到数据库
                // 赋值用户Id 相当于：39 1 放入数据库
                cartInfoNoLogin.setUserId(userId);
                cartInfoMapper.insert(cartInfoNoLogin);
            }
        }
        // 将合并之后的数据查询出来。
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    // 获取临时购物车中的数据userTempId ，也可以获取登录购物车数据 userId。
    private List<CartInfo> getCartList(String userId) {
        // 声明一个集合来存储数据
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 如果传入进来的用户Id 为空
        if (StringUtils.isEmpty(userId)) return cartInfoList;
        // 如果传入进来的用户Id不为空。
        // 先查询缓存
        // 必须先获取缓存的key
        String cartKey = getCartKey(userId);
        // 根据当前的购物车key 来获取缓存的数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        // 判断集合是否为空
        if (null!=cartInfoList && cartInfoList.size()>0){
            // 返回来的数据就是 List<CartInfo>
            // 还有一个细节 展示购物车数据的时候，应该有个排序规则。根据时间？ 更新时间并非创建时间。
            // 当前项目没有更新时间，所以在此模拟一个，按照Id 进行排序。
            cartInfoList.sort(new Comparator<CartInfo>() {
                // 比较器
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            // 返回当前的集合。
            return cartInfoList;
        }else {
            // 缓存没有数据，应该走数据库，并放入缓存。
            // 根据用户Id来查询数据
            cartInfoList = loadCartCache(userId);
            return cartInfoList;
        }
    }

    // 根据用户Id 查询数据库中的购物车数据，并添加到缓存！
    public List<CartInfo> loadCartCache(String userId) {
        // select * from cart_info where user_id = userId ;
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId);
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(cartInfoQueryWrapper);
        // 如果说数据库中没有数据。
        if (CollectionUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        // 如果数据库中有购物车列表。
        // 循环遍历，将集合中的每个cartInfo 放入缓存！
        // 一次放入多条数据
        HashMap<String, CartInfo> hashMap = new HashMap<>();

        for (CartInfo cartInfo : cartInfoList) {
            // hash 数据结构：hset(key,field,value) key=user:userId:cart field=skuId value=cartInfo字符串。
            // hash 数据结构：hmset(key,map); Map map = new HashMap(); map.put(field,value)
            // 细节：只要走到这个方法说明缓存失效了，既然缓存失效了。那么我们有必要查询一下最新价格，将数据放入缓存。
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            // 将数据放入map中。
            hashMap.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        // 在此将map放入缓存。
        // 获取缓存的key
        String cartKey = getCartKey(userId);
        redisTemplate.opsForHash().putAll(cartKey,hashMap);
        // 设置缓存过期时间
        setCartKeyExpire(cartKey);
        // 返回最终的数据。
        return cartInfoList;
    }

    // 设置购物车的过期时间
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    // 获取购物车中的cartKey
    private String getCartKey(String userId){
        // 区分是谁的购物车 user:userId:cart
        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
