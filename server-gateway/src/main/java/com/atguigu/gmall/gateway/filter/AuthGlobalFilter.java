package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.netflix.ribbon.proxy.annotation.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author mqx
 * 添加网关的过滤器
 * @date 2020/4/29 15:46
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Autowired
    private RedisTemplate redisTemplate;

    // 获取到请求资源的列表
    @Value("${authUrls.url}")
    private String authUrls;

    // 检查路劲匹配对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();
    /**
     *
     * @param exchange ServerWeb 的对象
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 先获取用户的请求对象 http://list.gmall.com/list.html?category3Id=61
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 内部接口 /**/inner/** 不允许外部访问。
        // http://localhost/api/product/inner/getSkuInfo/25
        if (antPathMatcher.match("/**/inner/**",path)){
            // 获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            // 不能访问
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 获取用户Id
        String userId = getUserId(request);
        // 获取临时用户Id
        String userTempId = getUserTempId(request);

        // 判断 /api/**/auth/** 如果是这样的路径，那么应该登录{用户缓存的userId}。
        if (antPathMatcher.match("/api/**/auth/**",path)){
            // 说明没有登录
            if (StringUtils.isEmpty(userId)){
                // 获取响应对象
                ServerHttpResponse response = exchange.getResponse();
                // 不能访问
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }
        // 验证用户请求的资源 Url ，未登录情况下不允许访问的路径配置配置文件中。
        if (null !=authUrls){
            // authUrls = trade.html,myOrder.html,list.html
            // 循环判断
            for (String authUrl : authUrls.split(",")) {
                // 判断path 中是否包含以上请求资源 如果请求资源中有trade.html，但是，用户未登录 提示用户登录。
                if (path.indexOf(authUrl)!=-1 && StringUtils.isEmpty(userId)){
                    // 获取响应对象
                    ServerHttpResponse response = exchange.getResponse();
                    // 赋值一个状态码
                    // 303 由于请求对应的资源，存在着另一个url，重定向。
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    // 重定向到登录链接
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                    return response.setComplete();
                }
            }
        }

        // 上述验证通过，需要将userId，传递到各个微服务上。
        // 如果用户没有登录，那么在添加购物车的时候，会产生一个临时用户Id，将临时的用户传递传递给各个微服务

        if (!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){
            // 传递登录用户Id
            if (!StringUtils.isEmpty(userId)){
                // 存储一个userId
                request.mutate().header("userId",userId);
            }
            // 传递登录临时用户Id
            if (!StringUtils.isEmpty(userTempId)){
                // 存储一个userId
                request.mutate().header("userTempId",userTempId);
            }
            // 将用户Id传递下去
            return chain.filter(exchange.mutate().request(request).build());
        }
        return chain.filter(exchange);
    }

    // 获取userId
    private String getUserId(ServerHttpRequest request) {
        // 用户Id 存储在缓存
        // key=user:login:token value=userId
        // 必须先获取token。 token 可能存在header,cookie 中。
        String token = "";
        List<String> tokenList = request.getHeaders().get("token");
        if (null!=tokenList && tokenList.size()>0){
            // 这个集合中只有一个key ，这个key token ，值对应的也是一个。
            token=tokenList.get(0);
        }else {
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //            List<HttpCookie> cookieList = cookies.get("token"); //表示获取cookie 中多个数据
            //            for (HttpCookie httpCookie : cookieList) {
            //                String value = httpCookie.getValue();
            //                // 添加到的集合中。
            //                list.add(value);
            //            }
            // 根据cookie 中的key 来获取数据
            HttpCookie cookie = cookies.getFirst("token");
            if (null!=cookie){
                token= URLDecoder.decode(cookie.getValue());
            }
        }
        if (!StringUtils.isEmpty(token)){
            // 才能从缓存中获取数据
            String userKey = "user:login:"+token;
            String userId = (String) redisTemplate.opsForValue().get(userKey);
            return userId;
        }
        return "";
    }

    // 提示信息
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        // 提示信息告诉用户 ,提示信息被封装到resultCodeEnum 对象
        // 将提示的信息封装到result 中。
        Result<Object> result = Result.build(null, resultCodeEnum);
        // 将result 转化为字符串
        String resultStr = JSONObject.toJSONString(result);
        // 将resultStr 转换成一个字节数组
        byte[] bytes = resultStr.getBytes(StandardCharsets.UTF_8);
        // 声明一个DataBuffer
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        // 设置信息输入格式
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        // 将信息输入到页面
        return response.writeWith(Mono.just(wrap));
    }

    // 在网关获取临时用户Id 用户在添加购物车中，必然的走网关
    private String getUserTempId(ServerHttpRequest request){
        String userTempId="";
        // 获取临时用户Id与获取用户Id 方法一致
        List<String> userTempIdList = request.getHeaders().get("userTempId");
        if (null!=userTempIdList){
            userTempId=userTempIdList.get(0);
        }else {
            // 从cookie中获取
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if (null!=cookie){
                userTempId = cookie.getValue();
            }
        }
        return userTempId;
    }
}
