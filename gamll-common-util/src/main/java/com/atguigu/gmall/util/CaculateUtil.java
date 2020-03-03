package com.atguigu.gmall.util;

import com.atguigu.gmall.bean.OmsCartItem;


import java.math.BigDecimal;
import java.util.List;

public class CaculateUtil {
    public static BigDecimal getAmount(List<OmsCartItem> omsCartItems){
        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();
            if (omsCartItem.getIsChecked().equals("1")) {
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;

    }
}
