package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.service.UserAddressService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author mqx
 * @date 2020/5/4 11:45
 */
@Service
public class UserAddressServiceImpl extends ServiceImpl<UserAddressMapper, UserAddress> implements UserAddressService {
    // 实现类服务层，通常调用mapper
    @Autowired
    private UserAddressMapper userAddressMapper;

    // 根据用户Id 查询List<UserAddress>
    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        // select * from user_address where user_id = ?
        return userAddressMapper.selectList(new QueryWrapper<UserAddress>().eq("user_id",userId));
    }


}
