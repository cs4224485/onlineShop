package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

public interface CartService {
    OmsCartItem ifCarExistByUser(String memberId, String skuId);

    void updateCart(OmsCartItem omsCartItemFromDB);

    void addCart(OmsCartItem omsCartItem);

    void flushCartCache(String memberId);

    List<OmsCartItem> cartList(String userId);

    void checkCart(OmsCartItem omsCartItem );
}
