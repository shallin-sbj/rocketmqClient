package com.scl.rocketmq.service;

import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 消费处理逻辑通过实现改接口实现
 */
public interface MessageProcessor {

    /**
     * 处理消息的接口
     * push消费模式使用
     *
     * @param messageExt
     * @return
     */
    default boolean handleMessage(MessageExt messageExt) {
        System.out.println("message:" + messageExt.getBody().toString());
        return true;
    }

    /**
     * 处理消息的接口
     * push消费模式使用
     *
     * @param messageExt
     * @return
     */
    default boolean handleMessage(MessageExt messageExt, String from) {
        System.out.println("from:" + from + "message:" + messageExt.getBody().toString());
        return true;
    }


    /**
     * 处理消息集合
     * push消费模式使用
     *
     * @param messageExtList
     * @return
     */
    default boolean handleMessageList(List<MessageExt> messageExtList) {
        for (MessageExt messageExt : messageExtList) {
            System.out.println("处理了：" + new String(messageExt.getBody()));
        }
        return true;
    }
}
