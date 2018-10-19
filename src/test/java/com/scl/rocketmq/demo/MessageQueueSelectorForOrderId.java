package com.scl.rocketmq.demo;

import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.List;

public class MessageQueueSelectorForOrderId  implements MessageQueueSelector{

    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        long l = Long.parseLong(String.valueOf(arg));
        Long id = Long.valueOf(l);
        long index = id % mqs.size();
        return mqs.get((int)index);
    }
}
