package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.apache.zookeeper.server.quorum.QuorumCnxManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMqListener {
    @Autowired
    OrderService orderService;

    @JmsListener(destination ="PAYMENT_SUCCESS_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage){
        try {
            String out_trade_no = mapMessage.getString("out_trade_no");
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setOrderSn(out_trade_no);
            System.out.println("支付成功更新订单信息");
            orderService.updateOrder(omsOrder);
            // 更新订单状态业务
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }
}
