package com.atguigu.gmall.redisson;

import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

@Controller
public class RedissonController {
    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RedisUtil redisUtil;

    @RequestMapping("testRedisson")
    @ResponseBody
    public String testRedisson() {
        RLock lock = redissonClient.getLock("lock");
        Jedis jedis = redisUtil.getJedis(); // 生命锁

        // 上锁
        lock.lock();
        try {

            String v = jedis.get("k");
            if (StringUtils.isBlank(v)) {
                v = "1";
            }
            System.out.println("->" + v);
            jedis.set("k", (Integer.parseInt(v) + 1) + "");
        } finally {
            jedis.close();
            // 解锁
            lock.unlock();
        }
        return "success";
    }

}
