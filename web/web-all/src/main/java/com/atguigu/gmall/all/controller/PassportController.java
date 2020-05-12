package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/4/29 15:17
 */
@Controller
public class PassportController {

    // http://passport.gmall.com/login.html?originUrl='+window.location.href
    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        // 从哪里点击的登录，应该跳回到哪里。
        // 从首页登录 http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
        String originUrl = request.getParameter("originUrl");
        // 需要保存originUrl ,因为前台需要跳转 originUrl: [[${originUrl}]]
        request.setAttribute("originUrl",originUrl);
        return "login";
    }
}
