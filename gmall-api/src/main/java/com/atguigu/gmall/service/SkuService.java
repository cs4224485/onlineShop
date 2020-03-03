package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSkuInfo;

import java.math.BigDecimal;
import java.util.List;

public interface SkuService {
    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);
    PmsSkuInfo getSkuInfo(String skuId);
    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);
    PmsSkuInfo getSkuInfoFromDB(String skuId);
    List<PmsSkuInfo> getAllSku(String catalog3Id);

    boolean checkPrice(String productSkuId, BigDecimal price);
}
