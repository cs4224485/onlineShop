package com.atguigu.gmall.mq;

import org.apache.activemq.command.ActiveMQMapMessage;


import javax.jms.*;

public class ActiveMQCommon {

    public static void createProducer(ActiveMQUtil activeMQUtil, String queueName, String messageKey, String messageValue)   {
        Connection connection = null;
        Session session = null;

        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);// 开启事务
            Queue queue = session.createQueue( queueName);
            MessageProducer producer = session.createProducer(queue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString(messageKey, messageValue);
            producer.send(mapMessage);
            session.commit();
        } catch (JMSException e) {
            try {
                session.rollback();
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                session.close();
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }


    }
}
