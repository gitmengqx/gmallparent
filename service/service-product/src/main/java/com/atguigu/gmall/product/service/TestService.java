package com.atguigu.gmall.product.service;

/**
 * @author mqx
 * @date 2020/4/22 15:13
 */
public interface TestService {
    // 测试本地锁
    void testLock();
    // 读锁
    String readLock();

    // 写锁
    String writeLock();
}
