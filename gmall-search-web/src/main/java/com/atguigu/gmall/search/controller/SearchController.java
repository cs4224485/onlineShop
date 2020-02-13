package com.atguigu.gmall.search.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.anootations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Controller
public class SearchController {

    @Reference
    SearchService searchService;

    @Reference
    AttrService attrService;

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {// 三级分类id、关键字、

        // 调用搜索服务，返回搜索结果
        List<PmsSearchSku> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfos);
        // 抽取搜索结果包含的平台属性集合
        Set<String> valueIdSet = new HashSet<>();
        for (PmsSearchSku pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue attrValue : skuAttrValueList) {
                String valueId = attrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }
        // 根据valueId将属性列表查询出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueByValueId(valueIdSet);
        modelMap.put("attrList", pmsBaseAttrInfos);


        // 对平台属性集合进一步处理，去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();

        // 处理Url参数拼接
        String urlParam = getUrlParam(pmsSearchParam,false);
        if (delValueIds != null) {
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            for (String delValueId : delValueIds) {
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    pmsSearchCrumb.setUrlParam(getUrlParam(pmsSearchParam,true, delValueId));
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String valueId = pmsBaseAttrValue.getId();
                        pmsSearchCrumb.setValueId(valueId);

                        if (delValueId.equals(valueId)) {
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            // 删除该属性值所在的属性组
                            iterator.remove();

                        }
                    }
                }
                modelMap.put("attrValueSelectedList", pmsSearchCrumbs);
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
        }
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

        modelMap.put("urlParam", urlParam);
        return "list";
    }


    private String getUrlParam(PmsSearchParam pmsSearchParam, Boolean isCrumbs, String... ValueIdDel) {
        // 数量平台属性value Url
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();
        String urlParam = "";
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                // 说明前面已经有参数了
                urlParam += "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                // 说明前面已经有参数了
                urlParam += "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if (skuAttrValueList != null) {
            for (String valueId : skuAttrValueList) {
                if (!isCrumbs) {
                    urlParam = urlParam + "&valueId=" + valueId;
                } else {
                    if (!ValueIdDel[0].equals(valueId)) {
                        urlParam = urlParam + "&valueId=" + valueId;
                    }
                }
            }

        }
        return urlParam;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index() {
        return "index";
    }
}

