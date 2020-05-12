package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author mqx
 * @date 2020/4/29 11:51
 */
public interface UserService {
    // 登录数据接口： select * from user_info where userName=? and pwd = ?
    // 对密码pwd 加密
    UserInfo login(UserInfo userInfo);

}
