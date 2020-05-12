package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author mqx
 * @date 2020/4/25 16:16
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    // 注入模板引擎
    @Autowired
    private SpringTemplateEngine templateEngine;

    /**
     * 如果没有这个静态页面，那么我们可以自己生产一个静态页面数据模板
     */
    @GetMapping("createHtml")
    @ResponseBody
    public Result createHtml() throws IOException {
        // 创建的时候，必须有三级分类数据
        Result result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        // result.getData() 表示分类数据
        context.setVariable("list",result.getData());
        // 在E盘根目录生成一个index.html 页面
        FileWriter fileWriter = new FileWriter("E:\\index.html");
        // 使用模板引擎
        templateEngine.process("index/index.html",context,fileWriter);
        return Result.ok();
    }

    // 访问首页
    // 用缓存
    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request){
        // 获取首页分类数据
        Result result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",result.getData());
        return "index/index";
    }
}
