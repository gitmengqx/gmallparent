package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * @date 2020/4/18 14:30
 */
@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    // 分页查询控制器 http://api.gmall.com/admin/product/baseTrademark/1/10
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long size){
        Page<BaseTrademark> param = new Page<>(page,size);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(param);
        // 返回分页之后的数据
        return Result.ok(baseTrademarkIPage);
    }

    // 根据Id 获取品牌：
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        // 获取数据
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    // 保存品牌：vue 项目通常都是传递的是json 字符串。 将json 对象转化为java对象
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        // 保存数据
        baseTrademarkService.save(baseTrademark);
        return Result.ok();
    }

    // 更新品牌：通常我们都需要传递一个Id 的主键
    @PutMapping("update")
    public Result updateById(@RequestBody BaseTrademark baseTrademark){
        // 更新
        baseTrademarkService.updateById(baseTrademark);
        // 返回
        return Result.ok();
    }
    // 删除品牌：
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        // delete from base_trade_mark where id = id
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    // 品牌：http://api.gmall.com/admin/product/baseTrademark/getTrademarkList
    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getTrademarkList();

        return Result.ok(baseTrademarkList);
    }


}
