package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * @date 2020/4/29 14:04
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;
    // 因为login.html 登录方法，登录方法提交的数据 this.user {json 数据}
    // 使用@RequestBody 转化为java 对象
    // 编写映射路径
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        // 暂时不返回页面！数据保存，用户信息。
        // 返回页面。在其他控制器处理。
        // http://passport.gmall.com/login.html?originUrl=http://item.gmall.com/15.html
        UserInfo info = userService.login(userInfo);
        // 用户在数据库中存在。
        if (null!=info){
            // 声明map 集合记录相关的数据
            HashMap<String, Object> hashMap = new HashMap<>();
            // 根据sso 的分析过程，用户登录之后的信息，应该放入缓存中，这样才能保证每个模块都可以访问到用户的信息。
            // 声明一个token
            String token = UUID.randomUUID().toString().replace("-","");
            // 记录token
            hashMap.put("token",token);
            // 用户昵称记录到map中。
            hashMap.put("nickName",info.getNickName());
            // 定义 key=user:login:token value = userId
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            redisTemplate.opsForValue().set(userKey,info.getId().toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            // 返回
            return Result.ok(hashMap);
        }else {
            return Result.fail().message("用户名或密码不正确！");
        }
    }
    // 退出登录
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        // 因为缓存中存储用户数据的时候，需要token，所以删除的时候，需要token组成key
        // 当登录成功之后，token 放入了，cookie，header中。
        // 从heaer 中获取token
        String token = request.getHeader("token");
        // 删除缓存中的数据
        // key=user:login:token value = userId
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX+token);
        // 最好的方式，清空 cookie 中的数据。
        return Result.ok();
    }


}
