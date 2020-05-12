package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author mqx
 * @date 2020/4/29 11:52
 */
@Service
public class UserServiceImpl implements UserService {
    // 服务层： select * from user_info where userName=? and pwd = ?
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        // 用户名，密码在数据库应该是唯一，返回只能是一个对象
        // 密码是加密的！
        // 获取用户在页面输入的密码
        String passwd = userInfo.getPasswd();
        // 对输入的密码进行加密，然后再跟数据库进行匹配
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName()).eq("passwd",newPwd);
        // 查询之后的对象
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (null!=info){
            // 说明数据库中有当前用户
            return info;
        }
        // 说明数据库中没有当前用户，返回null。
        return null;
    }
}
