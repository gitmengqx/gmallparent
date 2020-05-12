package com.atguigu.gmall.product.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author mqx
 * @date 2020/4/22 15:11
 */
@RestController
@RequestMapping("admin/product/test")
public class TestController {

    // 引入服务层
    @Autowired
    private TestService testService;

    @RequestMapping("testLock")
    public Result test(){
        testService.testLock();
        return Result.ok();
    }

    // 读锁
    @GetMapping("read")
    public Result read(){
        String msg = testService.readLock();
        return Result.ok(msg);
    }
    // 写锁
    @GetMapping("write")
    public Result write(){
        String msg = testService.writeLock();
        return Result.ok(msg);
    }


    // 直接编写一个main
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        String[] props =new String[]{"3:8G:运行内存"};
        System.out.println(JSON.toJSONString(props));

        // 创建一个线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(50, 500, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        // 并行化：
        // futureA ： 执行的结果 hello
        CompletableFuture<String> futureA = CompletableFuture.supplyAsync(() -> "hello");
        // futureB
        CompletableFuture<Void> futureB = futureA.thenAcceptAsync((s) -> {
            // 先线程睡一会
            delaySec(3);
            // 打印数据
            printCurrTime(s + " 第一个线程");
        }, threadPoolExecutor);
        // futureB
        CompletableFuture<Void> futureC = futureA.thenAcceptAsync((s) -> {
            // 先线程睡一会
            delaySec(1);
            // 打印数据
            printCurrTime(s + " 第二个线程");
        }, threadPoolExecutor);
        // futureB.futureC 都是依赖futureA的结果
        // 交替改变两个futureB，futureC的睡眠时间。看结果
        // futureB 睡了一秒钟，所以hello 第一个线程，futureC 睡了三秒钟 最后hello 第二个线程
        // 将futureB与futureC睡眠时间交换 ，这么执行结果也会互换，则说明是两个线程是并行关系！




//        // 支持返回值
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                System.out.println(Thread.currentThread().getName()+"----");
//                // 如果有异常：
//                // int i=1/0;
//                // 返回一个数值
//                return 1024;
//            }
//        }).thenApply(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer o) {
//                System.out.println("thenApply获取上一个任务返回的结果，并返回当前任务的返回值。"+o);
//                // 返回值
//                return o*2;
//            }
//        }).whenComplete(new BiConsumer<Object, Throwable>() {
//            // o:获取上一个结果：
//            // throwable：表示是否有异常
//            @Override
//            public void accept(Object o, Throwable throwable) {
//                System.out.println("o="+o+":========="+o.toString());
//                System.out.println("throwable="+throwable+"----------------");
//            }
//        }).exceptionally(new Function<Throwable, Integer>() {
//            @Override
//            public Integer apply(Throwable throwable) {
//                System.out.println("throwable="+throwable+":=========");
//                return 6666;
//            }
//        });
//        // 获取结果：
//        System.out.println(future.get());
    }

    private static void printCurrTime(String s) {
        System.out.println(s);
    }

    private static void delaySec(int i) {
        try {
            Thread.sleep(i*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
