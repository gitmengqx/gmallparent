package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author mqx
 * @date 2020/4/17 14:29
 */
@Service
public class ManageServiceImpl implements ManageService {

    // service 层调用mapper 层
    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private RabbitService rabbitService;


    @Override
    public List<BaseCategory1> getCategory1() {
        // select * from base_category1
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        // select * from base_category2 where category1_id = category1Id
        QueryWrapper<BaseCategory2> baseCategory2QueryWrapper = new QueryWrapper<>();
        baseCategory2QueryWrapper.eq("category1_id",category1Id);
        return baseCategory2Mapper.selectList(baseCategory2QueryWrapper);
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        // select * from base_category3 where category2_id = category1Id
        QueryWrapper<BaseCategory3> baseCategory3QueryWrapper = new QueryWrapper<>();
        baseCategory3QueryWrapper.eq("category2_id",category2Id);
        return baseCategory3Mapper.selectList(baseCategory3QueryWrapper);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        // 判断，用户到底是根据那一层的分类Id 查询的！
        // 编写一个xml
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
    }

    @Override
    @Transactional
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 区分：什么时候是新增，什么时候是修改
        /*
        base_attr_info : 平台属性
        base_attr_value ：平台属性值！
        */
        if (baseAttrInfo.getId()!=null){
            // 修改数据
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            // 只要在baseAttrInfo insert 了。那么我们就能够获取到baseAttrInfo的主键
            // @TableId(type = IdType.AUTO) 做了主键自增，并能够获取到自增之后的值。
            baseAttrInfoMapper.insert(baseAttrInfo); //  base_attr_info : 平台属性
        }

        // 修改平台属性值！ 最普遍的方式：update tName set cloumn = ? where id = xxx
        // 特殊的修改方式：因为我们不能确定用户到底修改的数据是谁！ 先删除数据，然后新增
        // delete from base_attr_value where attr_id = baseAttrInfo.getId();
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueQueryWrapper);

        // base_attr_value ：平台属性值！ 新增的操作
        // 得到的页面所要保存的平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList!=null && attrValueList.size()>0){
            // 循环添加
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // base_attr_value ：平台属性值！
                // id，valueName，attrId 页面提交过来的数据只有valueName
                // id 它是主键自增，attrId 是baseAttrInfo.getId();
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {
        // select * from base_attr_info where id = attrId
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        // 给 attrValueList 赋值
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {

        // select * from spu_info where category3_id = ? order by id desc
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.eq("category3_id",spuInfo.getCategory3Id());
        spuInfoQueryWrapper.orderByDesc("id");
        // 第一个参数 page 对象
        // 第二个参数 查询条件
        IPage<SpuInfo> spuInfoIPage = spuInfoMapper.selectPage(pageParam, spuInfoQueryWrapper);
        return spuInfoIPage;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        // select * from base_sale_attr
        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    public List<BaseTrademark> getTrademarkList() {
        //
        return null;
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
//        spuInfo: 商品表
        spuInfoMapper.insert(spuInfo);
//        spuSaleAttr: 销售属性表：
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                // 因为页面没有提供spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //        spuSaleAttrValue: 销售属性值表：
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        // 因为页面没有提供spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        // 页面提交时，没给销售属性名
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }

//        spuImage: 商品的图片表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null && spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                // 因为页面没有提供spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }

    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {
        // select * from spu_image where spu_id = spuId
        QueryWrapper<SpuImage> spuImageQueryWrapper = new QueryWrapper<>();
        spuImageQueryWrapper.eq("spu_id",spuId);
        return spuImageMapper.selectList(spuImageQueryWrapper);
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(Long spuId) {
        // 通过sql 语句查询销售属性，还需要查询销售属性值。
        // 因为销售属性，销售属性值存在不同的表中。所以此处建议使用xml形式进行多表关联查询。
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        // 定义skuInfoMapper
//        skuInfo: 库存单元表
        skuInfoMapper.insert(skuInfo);
//        skuAttrValue: 库存单元与平台属性，平台属性值的关系
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            // 循环插入
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                // 能否直接放入数据？ 添加库存单元Id skuId
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }
//        skuSaleAttrValue: 销售属性，销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            // 循环插入
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                // 注意:skuId , spuId 页面都没有提供
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                // 获取skuInfo 中的spuId 即可！
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());

                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }
//        skuImage：库存单元图片表。
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                // 赋值一个skuId
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }

        // 发送消息到rabbitmq，一个是exchange，一个是routingKey，一个消息。商品上架根据skuId 做的处理。
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuInfo.getId());

    }

    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoParam) {
        // 创建查询条件，我们这没有查询的条件，但是可以通过Wrapper 设置查询数据的排序。
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoParam,skuInfoQueryWrapper);

    }

    @Override
    public void onSale(Long skuId) {
        // update sku_info set is_sale=1 where id =  skuId
        // is_sale = 1 上架状态
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);
        // 发送消息到rabbitmq，一个是exchange，一个是routingKey，一个消息。商品上架根据skuId 做的处理。
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        /*
        上架、下架那里为什么不用先selectbyid，查到商品信息对象，再set是否销售的属性，而是采取new一个商品信息呢？
         */
        // 方法一：
        // update sku_info set is_sale=0 where id =  skuId
        // select * from sku_info where id = skuId;
        //        SkuInfo skuInfoSel = skuInfoMapper.selectById(skuId);
        //        skuInfoSel.setIsSale(0);
        //        skuInfoMapper.updateById(skuInfoSel);  update sku_info set is_sale=0 where id =  skuId

        // 方法二：
        // is_sale = 0 下架状态
        // update sku_info set is_sale=0 where id = skuId
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);

        // 发送商品的下架操作：
        // 发送消息到rabbitmq，一个是exchange，一个是routingKey，一个消息。商品上架根据skuId 做的处理。
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);

    }

    @Override
    @GmallCache(prefix = RedisConst.SKUKEY_PREFIX) // sku:
    public SkuInfo getSkuInfo(Long skuId) {
        // 利用redisson获取分布式锁查询数据库
        // return getSkuInfoRedisson(skuId);
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoRedisson(Long skuId) {
        SkuInfo skuInfo = null;
        try {
            // 定义存储商品的key
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 获取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            // 说明走数据库
            if (null==skuInfo){
                // 利用redisson 定义分布式锁
                // 定义分布式锁的key
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                RLock lock = redissonClient.getLock(lockKey);
                // 准备上锁
                /*
                lock.lock();
                lock.lock(10, TimeUnit.SECONDS);
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                 */
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                if (flag){
                    try {
                        // 业务逻辑代码
                        skuInfo = getSkuInfoDB(skuId);
                        // 为了防止缓存穿透
                        if (null==skuInfo){
                            SkuInfo skuInfo1 = new SkuInfo();
                            // 因为这个对象是空的。
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo1;
                        }
                        // 将数据库中的数据放入缓存
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo;

                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        lock.unlock();
                    }
                }else {
                    // 其他线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 调用查询方法。
                    return getSkuInfo(skuId);
                }
            }else {
                // 如果用户查询一个在数据库中根本不存在的数据时，那么我们存储一个空对象放入了缓存。
                // 实际上我们应该想要获取的是不是空对象，并且对象的属性也是有值的！
                if (null==skuInfo.getId()){
                    return null;
                }
                // 走缓存
                return skuInfo;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        // 防止缓存宕机，可以走数据库查询一下！
        return getSkuInfoDB(skuId);
    }
    // 利用redis 获取分布式锁 查询数据
    private SkuInfo getSkuInfoRedis(Long skuId) {
        SkuInfo skuInfo = null;
        try {
        /*
        1.  定义存储商品{sku} key = sku:skuId:info
        2.  去缓存中获取数据
         */
            String skuKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKUKEY_SUFFIX;
            // 获取数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);

            // 整合流程
            if(skuInfo==null){
                // 走db ，放入缓存。注意添加锁
                // 定义分布式锁的lockKey=sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX+skuId+RedisConst.SKULOCK_SUFFIX;
                // 获取一个随机字符串
                String uuid = UUID.randomUUID().toString();
                // 为了防止缓存击穿，执行分布式锁的命令
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
                // 判断是否添加锁成功
                // 获取到分布式锁！
                if (isExist){
                    // 获取到了分布式锁，走数据库查询数据并放入缓存。
                    System.out.println("获取到分布式锁");
                    skuInfo = getSkuInfoDB(skuId);
                    // 判断数据库中的数据是否为空
                    if (skuInfo==null){
                        // 为了防止缓存穿透，赋值一个空对象放入缓存。
                        SkuInfo skuInfo1 = new SkuInfo();
                        // 放入的超时时间。 24*60*60 一天 ，最好这个空对象的过期时间不要太长。
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);

                        return skuInfo1;
                    }
                    // 从数据库查询出来的数据要是不为空
                    redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    //  删除锁 定义的lua 脚本
                    String script="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptText(script);
                    // 根据锁的key 找锁的值，进行删除
                    redisTemplate.execute(redisScript, Arrays.asList(lockKey),uuid);
                    // 返回数据
                    return skuInfo;
                }else {
                    // 未获取到分布式锁，其他线程等待
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 调用查询方法。
                    return getSkuInfo(skuId);
                }
            }else{
                // 如果用户查询一个在数据库中根本不存在的数据时，那么我们存储一个空对象放入了缓存。
                // 实际上我们应该想要获取的是不是空对象，并且对象的属性也是有值的！
                if (null==skuInfo.getId()){
                    return null;
                }
                // 走缓存
                return skuInfo;
            }
        } catch (Exception e) {
            // 记录缓存宕机的日志，报警，管理员赶紧处理。
            e.printStackTrace();
        }
        // 如果缓存宕机了，那么我优先让应用程序访问数据库。
        // return skuInfo;
        return getSkuInfoDB(skuId);
    }

    // 根据skuId 查询数据库中的数据
    private SkuInfo getSkuInfoDB(Long skuId) {
        // select * from sku_info where id = skuId
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo!=null){
            // 根据skuId 查询图片列表数据
            // select * from sku_image where sku_id = skuId;
            QueryWrapper<SkuImage> skuImageQueryWrapper = new QueryWrapper<>();
            skuImageQueryWrapper.eq("sku_id",skuId);
            List<SkuImage> skuImageList = skuImageMapper.selectList(skuImageQueryWrapper);

            // 查询出来的集合赋值给skuInfo
            skuInfo.setSkuImageList(skuImageList);
        }
        // 这个skuInfo 中 不但有 skuNamge,weight,sku_default_image,imageList
        return skuInfo;
    }

    @Override
    @GmallCache(prefix = "categoryViewByCategory3Id:")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {
        // BaseCategoryView.id = BaseCategoryView.category3Id 是同一个值，那么id作为主键，也就是category3Id 是主键。
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "skuPrice:")
    public BigDecimal getSkuPrice(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        // 只要价格其他信息不需要
        if (null!=skuInfo){
            return skuInfo.getPrice();
        }else {
            // 返回初始值
            return new BigDecimal("0");
        }
       //  return new BigDecimal("0");
    }
    // 命名规则：接口中获取数据的时候，通常是getxxx();
    // 跟数据库有关系的查询 selectxxx()
    @Override
    @GmallCache(prefix = "spuSaleAttrListCheckBySku:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        // 多表关联查询：
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);

    }

    @Override
    @GmallCache(prefix = "saleAttrValuesBySpu:")
    public Map getSaleAttrValuesBySpu(Long spuId) {
        HashMap<Object, Object> hashMap = new HashMap<>();
        // 通过mapper查询数据
        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        if (mapList!=null && mapList.size()>0){
            for (Map map : mapList) {
                // value_ids 作为key，sku_id 作为value
                hashMap.put(map.get("value_ids"),map.get("sku_id"));
            }
        }
        return hashMap;
    }

    @Override
    @GmallCache(prefix = "baseCategoryList")
    public List<JSONObject> getBaseCategoryList() {
        List<JSONObject> list = new ArrayList<>();
        /*
        1.  先获取到所有的分类数据 一级，二级，三级分类数据
        2.  开始组装数据
                组装条件就是分类Id 为主外键
        3.  将组装的数据封装到 List<JSONObject> 数据中！
         */
        // 分类数据在视图中
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        // 按照一级分类Id 进行分组
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        // 定义一个index;
        int index = 1;
        // 获取一级分类的数据，一级分类的Id，一级分类的名称
        for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category1Map.entrySet()) {
            // 获取一级分类Id
            Long category1Id = entry1.getKey();
            // 放入一级分类Id
            // 声明一个对象
            JSONObject category1 = new JSONObject();
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            // 存储categoryName 数据
            List<BaseCategoryView> category2List = entry1.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            category1.put("categoryName",category1Name);

            // categoryChild 一会写！
            // 迭代index
            index++;
            // 获取二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            // 准备给二级分类数据 ，二级分类数据添加到一级分类的categoryChild中！
            List<JSONObject> category2Child = new ArrayList<>();
            // 二级分类数据可能有很多条数据
            for (Map.Entry<Long, List<BaseCategoryView>> entry2 : category2Map.entrySet()) {
                // 获取二级分类数据的Id
                Long category2Id = entry2.getKey();
                // 声明一个二级分类数据的对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                // 放入二级分类的名称
                List<BaseCategoryView> category3List = entry2.getValue();

                category2.put("categoryName",category3List.get(0).getCategory2Name());

                // 将二级分类数据添加到二级分类的集合中
                category2Child.add(category2);

                // 获取三级数据
                List<JSONObject> category3Child = new ArrayList<>();
                // 循环category3List 数据
                category3List.stream().forEach(category3View ->{
                    // 声明一个三级分类数据的对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    // 将三级分类数据添加到三级分类数据的集合
                    category3Child.add(category3);
                });

                // 二级中应该还有一个 categoryChild 添加的三级分类数据
                category2.put("categoryChild",category3Child);
            }
            // 将二级分类数据放入一级分类里面
            category1.put("categoryChild",category2Child);
            // 将所有的 category1 添加到集合中
            list.add(category1);
        }

        return list;
    }

    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long skuId) {
        // sku_attr_value base_attr_info base_attr_value 必然多表关联查询！
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

    // 获取平台属性值集合对象
    private List<BaseAttrValue> getAttrValueList(Long attrId){
        // select * from base_attr_value where attr_id = attrId
        QueryWrapper<BaseAttrValue> baseAttrValueQueryWrapper = new QueryWrapper<>();
        baseAttrValueQueryWrapper.eq("attr_id",attrId);
        return baseAttrValueMapper.selectList(baseAttrValueQueryWrapper);
    }
}
