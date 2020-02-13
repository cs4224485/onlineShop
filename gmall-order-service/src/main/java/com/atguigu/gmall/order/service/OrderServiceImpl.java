package com.atguigu.gmall.order.service;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;


    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        String tradeKey = "user:" + memberId + ":tradeCode";
        try {
            jedis = redisUtil.getJedis();
            String tradeCodeFromCache = jedis.get(tradeKey);
            // lua脚本
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));
            if (eval != null && eval != 0) {
                // jedis.del(tradeKey); // 如果要防止并发攻击需要使用lua脚本
                return "success";
            } else {
                return "fail";
            }
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }

    }

    @Override
    public String genTradeCode(String memberId) {
        String tradeCode = UUID.randomUUID().toString();
        String tradeKey = "user:" + memberId + ":tradeCode";
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            jedis.setex(tradeKey, 60 * 15, tradeCode);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return tradeCode;
    }
}
