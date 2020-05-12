package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @author mqx
 * @date 2020/4/27 14:02
 */
public interface SearchService {

    // 上架
    void upperGoods(Long skuId);
    // 下架
    void lowerGoods(Long skuId);
    // 更新热度排名
    void incrHotScore(Long skuId);
    // 根据用户的输入条件查询数据
    SearchResponseVo search(SearchParam searchParam) throws IOException;

}
