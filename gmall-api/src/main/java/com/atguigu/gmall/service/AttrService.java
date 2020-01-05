package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;

import java.util.List;

public interface AttrService {
    List<PmsBaseAttrInfo> getBaseAttrInfo(String catalog3Id);
    String saveBaseAttrInfo(PmsBaseAttrInfo attrInfo);
    List<PmsBaseAttrValue> getBaseAttrValue(String attrId);
    List<PmsBaseSaleAttr> baseSaleAttrList();
}
