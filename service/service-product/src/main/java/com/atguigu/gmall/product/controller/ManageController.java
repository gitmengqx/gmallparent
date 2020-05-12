package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * @date 2020/4/17 15:45
 */
@Api(tags = "商品的基础属性接口")
@RestController // @ResponseBody + @Controller
@RequestMapping("admin/product")
public class ManageController {

    // 必须注入实现类
    @Autowired
    private ManageService manageService;
    // 一级分类：
    @GetMapping("getCategory1")
    public Result getCategory1(){
        // 一级分类的集合数据
        List<BaseCategory1> baseCategory1List = manageService.getCategory1();
        // 将一级分类集合数据放入到Result 中的data 属性中！
        return Result.ok(baseCategory1List);
    }

    // 二级分类： getCategory2/1
    @GetMapping("getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> baseCategory2List = manageService.getCategory2(category1Id);
        return Result.ok(baseCategory2List);
    }
    // 三级分类： getCategory3/13
    @GetMapping("getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(@PathVariable Long category1Id,@PathVariable Long category2Id,
                               @PathVariable Long category3Id){

        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);
        // 返回数据
        return Result.ok(attrInfoList);

    }

    // 接收的数据应该是 BaseAttrInfo 中的每个属性组成的json 字符串
    // 后台系统页面是vue 制作，vue 保存的时候，传递过来的是Json 字符串。
    // 数据需要在后台用Java 对象接收 { json字符串 --@RequestBody--> JavaObject 【BaseAttrInfo】}
    // JavaObject---@ResponsBody--->Json 字符串
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        // 保存数据
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    // 修改平台属性：平台属性中有平台属性值
    // {attrId} --- @PathVariable Long attrId 如果传递的参数，与接收的参数一致。
    // 那么@PathVariable的value 可以省略。
    @GetMapping("getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){
        // attrId = baseAttrInfo.id
        // 根据业务需求：要先查询到平台属性对象。
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        // 从平台属性中获取平台属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        return Result.ok(attrValueList);
    }

    // http://api.gmall.com/admin/product/1/10?category3Id=2
    // 1 = page 10 = size  category3Id = 封装到spuInfo中 springmvc 对象传值
    // 根据三级分类Id 查询商品列表 带分页！
    @GetMapping("{page}/{size}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long size,
                        SpuInfo spuInfo){

        Page<SpuInfo> pageParam = new Page<>(page,size);
        // 调用分页查询数据！
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam, spuInfo);
        // 返回结果集
        return Result.ok(spuInfoIPage);
    }
}
