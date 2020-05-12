package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mqx
 * @date 2020/4/24 16:15
 */
@Target(ElementType.METHOD) // 该注解使用在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解的生命周期
public @interface GmallCache {

    // 定义一个字段：作为缓存的key来使用的！
    // 以后做缓存的时候 缓存的key 由sku:组成，sku:是缓存key的一部分。
    String prefix() default "cache";
}
