package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author mqx
 * 可以操作elasticSearch CRUD
 * @date 2020/4/27 14:09
 */
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
