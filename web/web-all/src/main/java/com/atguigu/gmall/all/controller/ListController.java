package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/29 8:56
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;
    // http://list.gmall.com/list.html?keyword=手机
    // http://list.gmall.com/list.html?category3Id=61
    // springmvc 对象传值规则 ？后面传递的参数 与接收对象的属性名称一致，那么会自动映射。
    // @RequestMapping("list.html") 能够接收post,get请求。
//    @RequestMapping("list.html")
    @GetMapping("list.html")
    public String list(SearchParam searchParam, Model model){
        // 将数据保存，在index.html渲染。
        // 数据从何处来? service-list
        Result<Map> result = listFeignClient.list(searchParam);
        model.addAllAttributes(result.getData());

        // 获取品牌的传递过来的参数
        String trademark = getTrademark(searchParam.getTrademark());

        // 获取平台属性
        List<Map<String, String>> propsList = getMakeProps(searchParam.getProps());

        // 获取排序规则：
        Map<String, Object> map = getOrder(searchParam.getOrder());

        // 页面渲染的时候需要 urlParam 主要作用就记录拼接 url参数列表。
        // searchParam 接收用户查询条件。
        String urlParam = makeUrlParam(searchParam);

        // 保存用户查询数据
        model.addAttribute("searchParam",searchParam);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("trademarkParam",trademark);
        // 存储平台属性
        model.addAttribute("propsParamList",propsList);
        // 存储排序规则
        model.addAttribute("orderMap",map);
        return "list/index";
    }

    // 记录查询的条件
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        // 判断是否根据关键字
        // http://list.gmall.com/list.html?keyword=手机
        if (searchParam.getKeyword()!=null){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        // 判断是否根据分类Id 查询
        // http://list.gmall.com/list.html?category1Id=2
        if (searchParam.getCategory1Id()!=null){
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        // http://list.gmall.com/list.html?category2Id=13
        if (searchParam.getCategory2Id()!=null){
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        // http://list.gmall.com/list.html?category3Id=61
        if (searchParam.getCategory3Id()!=null){
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        // 判断品牌
        // http://list.gmall.com/list.html?category3Id=61&trademark=2:华为
//        if (searchParam.getTrademark()!=null && searchParam.getTrademark().length()>0){
//            urlParam.append("&trademark=").append(searchParam.getTrademark());
//        }
        if (searchParam.getTrademark()!=null){
            if (searchParam.getTrademark().length()>0){
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        // 判断平台属性值
        // http://list.gmall.com/list.html?category3Id=61&trademark=2:华为&props=1:2800-4499:价格
        if (searchParam.getProps()!=null){
            for (String prop : searchParam.getProps()) {
                if (urlParam.length()>0){
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        // 记录的拼接条件
        String urlParamStr = urlParam.toString();
        return "list.html?"+urlParamStr;
    }
    // 获取品牌名称 品牌：品牌名称 trademark=2:华为
    private String getTrademark(String trademark){
        // 使用工具类判断StringUtils
        if (trademark!=null && trademark.length()>0){
            // 将字符串进行分割 trademark=2:华为
            // String[] split = trademark.split(":");
            String[] split = StringUtils.split(trademark, ":");
            // 符合数据格式
            // if (split!=null && split.length==2){
            if (split!=null && split.length==2){
                return "品牌："+ split[1];
            }
        }
        return "";
    }
    // 获取平台属性值过滤得到面包屑
    // 传入的参数应该是个数组
    // props=1:2800-4499:价格&props=2:6.75-6.84英寸:屏幕尺寸
    private List<Map<String,String>> getMakeProps(String[] props){
        List<Map<String,String>> list = new ArrayList<>();
        // 判断传递过来的数据是否为空
        if (props!=null && props.length>0){
            // 循环获取里面的数据
            for (String prop : props) {
                // prop 每个值 组成格式 [2:6.75-6.84英寸:屏幕尺寸]
                // 进行分割
                String[] split = prop.split(":");
                // 循环这个数组中的每个数据 符合数据格式
                if (split!=null && split.length==3){
                    // 将字符串中的每个值放入map中
                    // 保存平台属性Id，平台属性值的名称，平台属性名
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);

                    // 将map 放入一个集合
                    list.add(map);
                }
            }
        }
        return list;
    }
    // 获取排序规则
    //
    //  http://list.gmall.com/list.html?category3Id=61&order=2:asc
    //  http://list.gmall.com/list.html?category3Id=61&order=2:desc
    private Map<String ,Object> getOrder(String order){
        HashMap<String, Object> map = new HashMap<>();
        // 判断不为空
        if (StringUtils.isNotEmpty(order)){
            // order=2:asc
            String[] split = order.split(":");
            // 符合格式
            if (split!=null && split.length==2){
                // type 代表的是用户点击的哪个字段
                map.put("type",split[0]);
                // sort 代表排序规则
                map.put("sort",split[1]);
            }
        }else { //  http://list.gmall.com/list.html?category3Id=61&order=
            // 如果没有排序规则
            // type 代表的是用户点击的哪个字段
            map.put("type","1");
            // sort 代表排序规则
            map.put("sort","asc");
        }
        return map;
    }

}
