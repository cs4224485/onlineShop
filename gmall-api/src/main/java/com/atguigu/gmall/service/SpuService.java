package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;

import java.util.List;

public interface SpuService {
    List<PmsProductInfo> spuList(String catalog3Id);
    String saveSpu(PmsProductInfo pmsProductInfo);
    List<PmsProductSaleAttr> spuSaleAttrList(String spuId);
    List<PmsProductImage> spuImageList(String spuId);
    List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId, String id);
}
