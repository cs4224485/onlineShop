package com.atguigu.gmall.manage.service.Impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 保存图片数据
        List<PmsSkuImage> pmsSkuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage skuImage:pmsSkuImageList){
            skuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(skuImage);
        }
        // 保存平台属性值
        List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue attrValue:pmsSkuAttrValueList){
            attrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(attrValue);
        }

        // 保存销售属性
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue saleAttrValue:pmsSkuSaleAttrValueList){
            saleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(saleAttrValue);
        }
    }

    @Override
    public PmsSkuInfo getSkuInfoFromDB(String skuId) {
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectOne(skuInfo);
        // 获取sku商品图片
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> skuImageList = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo.setSkuImageList(skuImageList);
        return pmsSkuInfo;
    }

    @Override
    public PmsSkuInfo getSkuInfo(String skuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        // 链接缓存
        Jedis jedis = redisUtil.getJedis();
        // 查询缓存
        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);
        if(StringUtils.isNoneBlank(skuJson)){
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            // 如果缓存中没有，查询mysql
            // 设置分布式锁
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("sku"+skuId+":lock", token, "nx", "px", 10*1000); // 拿到锁的线程有10秒的过期时间
            if(StringUtils.isNoneBlank(OK) && OK.equals("OK")){
                // 设置成功，有权在10秒的过期时间内访问数据库
                pmsSkuInfo = getSkuInfoFromDB(skuId);
                if(pmsSkuInfo != null){
                    // mysql查询结果存入redis
                    jedis.set("sku:" + skuId + ":info", JSON.toJSONString(pmsSkuInfo));
                }else {
                    // 数据库中不存在该sku
                    // 为了防止缓存穿透, 将null或者空字符串值设置给redis
                    jedis.setex("sku"+skuId+":info", 60*3, JSON.toJSONString(""));
                }
                // 在访问mysql后, 将mysql的分布锁释放
                String lockToken = jedis.get("sku" + skuId + ":lock");
                if(StringUtils.isNoneBlank(lockToken) && lockToken.equals(token)){
                    // jedis.eval("lua"); 可使用lua脚本，在查询到key的同时删除key,防止高并发下的意外发生
                    // 用token确认删除的是自己的sku的锁
                    jedis.del("sku"+skuId+":lock");
                }

            }else {
                // 设置失败，自旋(该线程在睡眠几秒后，重新尝试访问本方法)
                try {
                    Thread.sleep(3000);

                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                return getSkuInfo(skuId);
            }
        }
        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        return pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        // 根据三级分类获取SKU
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setCatalog3Id(catalog3Id);
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.select(skuInfo);
        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();
            PmsSkuAttrValue attrValue = new PmsSkuAttrValue();
            attrValue.setSkuId(skuId);
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.select(attrValue);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectOne(skuInfo);
        BigDecimal dbPrice = pmsSkuInfo.getPrice();
        if (dbPrice.compareTo(price) == 0 ){
            return true;
        }

        return false;
    }
}
