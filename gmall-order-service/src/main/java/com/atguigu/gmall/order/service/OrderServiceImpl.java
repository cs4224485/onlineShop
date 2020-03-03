package com.atguigu.gmall.order.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;

import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper orderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    CartService cartService;

    @Autowired
    ActiveMQUtil activeMQUtil;

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
            System.out.println(tradeKey);
            jedis.setex(tradeKey, 60 * 15, tradeCode);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return tradeCode;
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
        orderMapper.insertSelective(omsOrder);
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        String orderId = omsOrder.getId();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            // 从购物车清除出去
            cartService.delProductSku(omsOrderItem);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    @Override
    public void updateOrder(OmsOrder omsOrder) {
        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());
        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus("1");

        // 发送一个订单已支付的队列, 提供给库存系统消费
        Connection connection = null;
        Session session = null;

        try{
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
            Queue payment_success_queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(payment_success_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            // 查询订单的对象，转化成json字符串，存入ORDER_PAY_QUEUE的消息队列
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderResponse = orderMapper.selectOne(omsOrderParam);

            OmsOrderItem omsOrderItemParam = new OmsOrderItem();
            omsOrderItemParam.setOrderSn(omsOrderParam.getOrderSn());
            List<OmsOrderItem> select = omsOrderItemMapper.select(omsOrderItemParam);
            omsOrderResponse.setOmsOrderItems(select);
            activeMQTextMessage.setText(JSON.toJSONString(omsOrderResponse));
            orderMapper.updateByExampleSelective(omsOrderUpdate,example);
            producer.send( activeMQTextMessage);
            session.commit();
        }catch (Exception e){
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        return orderMapper.selectOne(omsOrder);
    }
}
