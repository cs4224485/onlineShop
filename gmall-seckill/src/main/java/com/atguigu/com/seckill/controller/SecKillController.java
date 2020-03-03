package com.atguigu.com.seckill.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@Controller
public class SecKillController {
    @Autowired
    RedisUtil redisUtil;

    @RequestMapping("kill")
    @ResponseBody
    public String kill(){
        String memberId = "1";
        Jedis jedis = redisUtil.getJedis();
        // 开启商品的监控
        jedis.watch("106");
        Integer stock = Integer.parseInt(jedis.get("106" ));
        if(stock>0){
            Transaction multi = jedis.multi();
            multi.incrBy("106", -1);
            List<Object> exec = multi.exec();
            if(exec!=null&&exec.size()>0){
                System.out.println("当前库存剩余数量"+stock+",用户"+memberId+"抢购成功");
            }

        }
        return "1";
    }

}
