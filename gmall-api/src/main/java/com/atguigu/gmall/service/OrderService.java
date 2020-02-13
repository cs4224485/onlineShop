package com.atguigu.gmall.service;

public interface OrderService {
    String checkTradeCode(String memberId,String tradeCode);

    String genTradeCode(String memberId );
}
