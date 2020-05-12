package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/17 14:18
 */
public interface ManageService {
    // 编写抽象方法

    /**
     * 查询所有的一级分类数据
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类Id 查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    // 查询平台属性：可能跟一级分类Id，二级分类Id，三级分类Id 都有关系！

    /**
     * 根据分类Id 查询平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id,Long category2Id,Long category3Id);


    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据attrId 查询平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    // 分页查询

    /**
     * 根据分类Id 进行分页查询数据
     * @param pageParam 封装第几页，每页显示的条数
     * @param spuInfo 查询条件
     * @return
     */
    IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam,SpuInfo spuInfo);

    /**
     * 查询所有的销售属性
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 查询品牌
     * @return
     */
    List<BaseTrademark> getTrademarkList();

    /**
     * 保存商品
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据商品Id 查询图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 根据spuId 查询销售属性对象集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> spuSaleAttrList(Long spuId);

    /**
     * 保存库存单元信息
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 查询skuInfo 数据
     * @param skuInfoParam
     * @return
     */
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoParam);

    /**
     * 根据skuId 上架操作
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 根据skuId 下架操作
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据skuId 查询skuInfo 信息。
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);


    /**
     * 根据三级分类Id 查询分类的名称
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 根据skuId 查询价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据skuId spuId 查询销售属性，销售属性值
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@Param("skuId") Long skuId, @Param("spuId")Long spuId);

    /**
     * 根据spuId 获取销售属性值Id与skuId 组成的集合数据
     * @param spuId
     * @return
     */
    Map getSaleAttrValuesBySpu(Long spuId);

    /**
     * 查询所有的分类数据
     * @return
     */
    List<JSONObject> getBaseCategoryList();

    /**
     * 根据品牌Id 查询品牌数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     * 传入商品Id 获取平台属性，属性值的名称。
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long skuId);


}
