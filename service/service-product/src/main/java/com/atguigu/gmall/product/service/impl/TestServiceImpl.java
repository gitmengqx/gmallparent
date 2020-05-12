package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * @date 2020/4/22 15:13
 */
@Service
public class TestServiceImpl implements TestService {

    // 引入操作缓存的对象
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 引入RedissonClient
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public void testLock() {
        // 创建锁：
        String skuId="25";
        String locKey ="lock:"+skuId;
        // 锁的是每个商品
        RLock lock = redissonClient.getLock(locKey);
        // 开始加锁 最常见的使用方法。
        // lock.lock();
        // 表示10秒之后自动解锁。
        lock.lock(10,TimeUnit.SECONDS);
        // 最多等待时间
//                boolean flag = false;
//                try {
//                    flag = lock.tryLock(100, 10, TimeUnit.SECONDS);
//                    if (flag){
//                        // 业务逻辑代码
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }finally {
//                    lock.unlock();
//                }

        // 业务逻辑代码
        // 获取数据
        String value = redisTemplate.opsForValue().get("num");
        if (StringUtils.isBlank(value)){
            return;
        }
        // 将value 变为int
        int num = Integer.parseInt(value);
        // 将num +1 放入缓存
        redisTemplate.opsForValue().set("num",String.valueOf(++num));
        // 解锁：
        lock.unlock();
    }

    @Override
    public String readLock() {
        // 获取读写锁对象
        RReadWriteLock readwriteLock = redissonClient.getReadWriteLock("readwriteLock");
        // 获取读锁
        RLock rLock = readwriteLock.readLock();
        // 开始加锁 给一个锁的时间，10秒后解锁
        rLock.lock(10,TimeUnit.SECONDS);
        // 读取缓存的数据
        String msg = redisTemplate.opsForValue().get("msg");

        // 返回数据
        return msg;
    }

    @Override
    public String writeLock() {
        // 读写锁，他们的key 必须一致。
        // 获取读写锁对象
        RReadWriteLock readwriteLock = redissonClient.getReadWriteLock("readwriteLock");
        // 获取写锁
        RLock rLock = readwriteLock.writeLock();
        // 加锁锁 10秒之后自动解锁
        rLock.lock(10,TimeUnit.SECONDS);
        // 写入缓存数据
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());

        return "写入数据完成...";
    }


//    // synchronized
//    @Override
//    public void testLock() {
//        /*
//        1.  在缓存redis中设置一个key=num ，并且给key 做一个初始化值value=0
//            set(num,0);
//        2.  查询num 中是否有值
//            2.1 true: 那么num 加1
//            2.2 false: 直接返回
//         */
//        // 使用 setnx() 命令。
//
//        // 声明一个uuid ,将做为一个value 放入我们的key所对应的值中
//        String uuid = UUID.randomUUID().toString();
//        // 定义一个锁：lua 脚本可以使用同一把锁，来实现删除！
//        String skuId = "25"; // 访问skuId 为25号的商品 100008348542
//        String locKey ="lock:"+skuId; // 锁住的是每个商品的数据
//        // 如果有10个线程进来访问25  lock:25
//        // 如果有10个线程进来访问26  lock:26
//        // 每一个商品都对应的有一把锁：而这个锁是由商品Id 组成的。
//        // 这个命令具有原子性： 如果该命令执行成功，则有过期时间，如果命令没有执行成功，没有过期时间。
////        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid,3,TimeUnit.SECONDS);
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent(locKey, uuid,3,TimeUnit.SECONDS);
//
//        // 第一种： lock 与过期时间中间不写任何的代码。
//        // redisTemplate.expire("lock",10, TimeUnit.SECONDS);//设置过期时间
//        // 如果true
//        if (lock){
//            // 执行的业务逻辑开始
//            // 获取缓存中的num 数据
//            String value = redisTemplate.opsForValue().get("num");
//            // 如果是空直接返回
//            if (StringUtils.isBlank(value)){
//                return;
//            }
//            // 不是空 如果说在这出现了异常！ 那么delete 就删除失败！ 也就是说锁永远存在！
//            int num = Integer.parseInt(value);
//            // 使num 每次+1 放入缓存
//            redisTemplate.opsForValue().set("num",String.valueOf(++num));
//            // 执行业务逻辑完成。
//            // 第一种：
//            // redisTemplate.expire("lock",10, TimeUnit.SECONDS);//设置过期时间
//            // 判断：当前线程key，所对应的value 是不是自己的值。
////            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
////                // 删除锁：
////                redisTemplate.delete("lock");
////            }
//            /*使用lua脚本来锁*/
//            // 定义lua 脚本
//            String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
//            // 使用redis执行lua执行
//            // 第一种传值
//            // DefaultRedisScript<Object> redisScript = new DefaultRedisScript<>(script);
//            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
//            // 第二种传值
//            redisScript.setScriptText(script);
//            // 设置一下返回值类型 为Long
//            // 因为删除判断的时候，返回的0,给其封装为数据类型。如果不封装那么默认返回String 类型，那么返回字符串与0 会有发生错误。
//            redisScript.setResultType(Long.class);
//            // 第一个要是script 脚本 ，第二个需要判断的key，第三个就是key所对应的值。
//            redisTemplate.execute(redisScript, Arrays.asList(locKey),uuid);
//
//        }else {
//            // 其他线程等待
//            try {
//                // 睡眠
//                Thread.sleep(1000);
//                // 睡醒了之后，调用方法。
//                testLock();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
