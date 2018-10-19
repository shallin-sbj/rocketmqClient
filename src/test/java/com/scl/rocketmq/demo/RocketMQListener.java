package com.scl.rocketmq.demo;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * MQ消息的监听接口类
 */
public class RocketMQListener implements MessageListenerConcurrently {

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        for (MessageExt message : list) {
            String msg = new String(message.getBody());
            System.out.println("msg data from rocketMQ:" + msg);
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
