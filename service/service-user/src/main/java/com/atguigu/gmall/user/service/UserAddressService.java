package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author mqx
 * 根据用户Id 查询数据
 * select * from user_address where user_id = ?
 * IService  com.baomidou.mybatisplus.extension.service 这个接口中有很多自定义的方法。比如：removeById updateById
 * @date 2020/5/4 11:42
 */
public interface UserAddressService extends IService<UserAddress> {
    // 如果有删除或者编辑 removeById updateById
    // 自定义一个方法select * from user_address where user_id = ?
    List<UserAddress> findUserAddressListByUserId(String userId);

}
