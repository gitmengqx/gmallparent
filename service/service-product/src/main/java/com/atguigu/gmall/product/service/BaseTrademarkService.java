package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author mqx
 * @date 2020/4/18 14:15
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    // IService 内部定义好了许多对单表进行操作的方法

    // 继承了IService 之后对单张表的CRUD 一些方法不需要写方法。

    // 自定义一个分页查询方法 分页查询
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> param);

    // 查询所有的品牌
    List<BaseTrademark> getTrademarkList();
}
