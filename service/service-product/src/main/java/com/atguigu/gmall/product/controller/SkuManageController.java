package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mqx
 * @date 2020/4/20 11:27
 */
@Api(tags = "sku接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    // 调用服务层对象
    @Autowired
    private ManageService manageService;

    // 查询图片列表 http://api.gmall.com/admin/product/spuImageList/9
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId){
        // 根据spuId 查询图片列表
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        // 返回数据
        return Result.ok(spuImageList);
    }

    // 加载销售属性数据 http://api.gmall.com/admin/product/spuSaleAttrList/9
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId){
        // 调用服务层方法
        List<SpuSaleAttr> spuSaleAttrList = manageService.spuSaleAttrList(spuId);

        // 返回数据
        return Result.ok(spuSaleAttrList);
    }

    // http://api.gmall.com/admin/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        // 保存skuInfo 信息
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/list/1/5
    @GetMapping("list/{page}/{size}")
    public Result skuInfoList(@PathVariable Long page,
                              @PathVariable Long size){

        // 将前台页面传递过来的参数设置到分页类中
        Page<SkuInfo> skuInfoParam = new Page<>(page, size);
        // 调用服务层的分页方法。
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(skuInfoParam);

        // 返回分页之后的数据集合
        return Result.ok(skuInfoIPage);
    }
    // http://api.gmall.com/admin/product/onSale/23
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);

        return Result.ok();
    }
    // http://api.gmall.com/admin/product/cancelSale/21
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        manageService.cancelSale(skuId);
        // 比如说：
        return Result.ok();
    }


}
