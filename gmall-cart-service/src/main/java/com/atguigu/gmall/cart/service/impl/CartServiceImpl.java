package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem ifCarExistByUser(String memberId, String skuId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        OmsCartItem omsCartItem1 = omsCartItemMapper.selectOne(omsCartItem);
        return omsCartItem1;
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDB) {
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("id", omsCartItemFromDB.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDB,e);

    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        if(StringUtils.isNotBlank(omsCartItem.getMemberId())){
            omsCartItemMapper.insert(omsCartItem);
        }
    }

    @Override
    public void flushCartCache(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        // 同步到redis缓存中
        Jedis jedis = redisUtil.getJedis();
        HashMap<String, String> map = new HashMap<>();
        for(OmsCartItem cartItem : omsCartItems){
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }
        jedis.del("user:"+memberId+":cart");
        jedis.hmset("user:"+memberId+":cart", map);
        jedis.close();
    }

    @Override
    public List<OmsCartItem> cartList(String userId) {
        Jedis jedis = null;
        List<OmsCartItem> cartItemList = new ArrayList<>();
        try{
            jedis = redisUtil.getJedis();
            // 用户购物车在redis的集合
            List<String> hvals = jedis.hvals("user:" + userId + ":cart");
            for (String hval : hvals) {
                OmsCartItem cartItem = JSON.parseObject(hval, OmsCartItem.class);
                cartItemList.add(cartItem);
            }
        }catch (Exception e){
            // 处理异常，记录系统日志
            e.printStackTrace();
            String message = e.getMessage();
        }finally {
            jedis.close();
        }
        return cartItemList;
    }

    @Override
    public void checkCart(OmsCartItem omsCartItem ) {
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId", omsCartItem.getMemberId()).andEqualTo( "productSkuId", omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem, e);
        flushCartCache(omsCartItem.getMemberId());
    }

    @Override
    public void delProductSku(OmsOrderItem omsOrderItem) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setProductSkuId(omsOrderItem.getProductSkuId());
        OmsCartItem omsCartItem1 = omsCartItemMapper.selectOne(omsCartItem);
        if (omsCartItem1 != null){
            omsCartItemMapper.delete(omsCartItem);
        }
    }
}
