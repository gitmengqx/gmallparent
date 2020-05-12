package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * 利用 aop来实现缓存
 * @date 2020/4/24 16:25
 */
@Component
@Aspect
public class GmallCacheAspect {
    // 引入redis，redissonClient
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    // 编写一个方法 利用环绕通知来获取对应的数据
    // 模拟@Transactional 用法
    // 返回值类型Object 因为我们在切方法的时候，不能确定方法的返回值到底是什么？
    //  SkuInfo getSkuInfo(Long skuId) 返回是SkuInfo
    //  BigDecimal getSkuPrice(Long skuId) 返回的BigDecimal
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point){
        // 声明一个Object
        Object result = null;
        // http://item.gmall.com/22.html  prefix = RedisConst.SKUKEY_PREFIX sku
        // 以后缓存的key 形式 sku:[22] sku:[skuId]
        // 获取传递的过来的参数
        Object[] args = point.getArgs();
        // 获取方法上的签名，我们如何知道方法上是否有注解
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        // 得到注解
        GmallCache gmallCache = methodSignature.getMethod().getAnnotation(GmallCache.class);
        // 获取缓存的前缀
        String prefix = gmallCache.prefix();
        // 组成缓存的key sku:[22] sku:[skuId]
        String key = prefix+ Arrays.asList(args).toString();
        // 从缓存中获取数据
        result = cacheHit(key,methodSignature);

        // 判断 缓存中有数据
        if (result!=null){
            return result;
        }
        // 缓存没有数据
        // 上锁 分布式锁
        RLock lock = redissonClient.getLock(key + ":lock");
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if (res){
                // 获取到分布式锁，从数据库中获取数据
                // 如果访问getSkuInfoDB 那么它相当于 调用skuInfoDB
                result = point.proceed(point.getArgs());
                // 判断result 是否为null 防止缓存穿透
                if (null == result){
                    Object o = new Object();
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return o;
                }
                redisTemplate.opsForValue().set(key,JSONObject.toJSONString(result), RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                return result;
            }else {
                Thread.sleep(1000);
                // 自旋
                return cacheAroundAdvice(point);
                // 直接获取缓存数据
                // return cacheHit(key,methodSignature); // SkuInfo
            }
        }catch (Exception e){
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }finally {
            // 因为我们放入数据的时候，放入的Object。我们不能确保Object 中是否有Id属性
            // 如果缓存的getSkuInfoDB(), 这个方法返回值 SkuInfo ,对于这个方法就有Id，能判断。
            // 但是，如果缓存getSkuPrice(); 这个方法返回值 BigDecimal ，对于这个方法就没有Id，不能判断。
            // 解锁
            lock.unlock();
        }
        return result;
    }
    // 从缓存获取数据
    private Object cacheHit(String key,MethodSignature methodSignature) {
        // 必须有key | 缓存放入数据的时候Object ，但是，我们可以看做是一个字符串。
        // 如果是访问的skuId 在数据库中不存在，空的Object
        String cache = (String) redisTemplate.opsForValue().get(key);
        // 判断当前的字符串是否有值
        if (StringUtils.isNotBlank(cache)){
            // 字符串是项目中多需要的那种数据类型，我们要确定？
            //  SkuInfo getSkuInfo(Long skuId) 返回是SkuInfo
            //  BigDecimal getSkuPrice(Long skuId) 返回的BigDecimal
            //  总结：方法的返回值类型是什么，那么缓存就是存储的什么数据类型！
            Class returnType = methodSignature.getReturnType();
            // 现在需要将cache 转换成方法的返回值类型即可！
            return JSONObject.parseObject(cache,returnType); // 返回的是啥。空的SkuInfo 对象。
        }
        return null;
    }


}
