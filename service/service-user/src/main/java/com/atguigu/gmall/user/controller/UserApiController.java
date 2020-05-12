package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author mqx
 * @date 2020/5/4 11:51
 */
@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private UserAddressService userAddressService;

    // 根据用户Id 查询用户收货地址列表.*****
    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable String userId){
        return userAddressService.findUserAddressListByUserId(userId);
    }

    // 编辑方法
    @GetMapping("inner/updateUserAddressById/{Id}")
    public Result updateUserAddressById(@PathVariable Long Id){
        UserAddress userAddress = new UserAddress();
        userAddress.setId(Id);
        userAddressService.updateById(userAddress);
        return Result.ok();
    }
    // 删除方法
    @GetMapping("inner/removeUserAddressById/{Id}")
    public Result removeUserAddressById(@PathVariable Long Id){
        userAddressService.removeById(Id);
        return Result.ok();
    }


}
