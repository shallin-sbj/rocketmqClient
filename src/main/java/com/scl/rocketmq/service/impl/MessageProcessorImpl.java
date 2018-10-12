package com.scl.rocketmq.service.impl;

import com.scl.rocketmq.service.MessageProcessor;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageProcessorImpl implements MessageProcessor {

    @Override
    public boolean handleMessage(MessageExt messageExt) {
        System.out.println("message:" + messageExt.getBody().toString());
        return true;
    }

    @Override
    public boolean handleMessage(MessageExt messageExt, String from) {
        System.out.println("from:" + from + "message:" + messageExt.getBody().toString());
        return true;
    }

    @Override
    public boolean handleMessageList(List<MessageExt> messageExtList) {
        for (MessageExt messageExt : messageExtList) {
            System.out.println("处理了：" + new String(messageExt.getBody()));
        }
        return true;
    }
}
