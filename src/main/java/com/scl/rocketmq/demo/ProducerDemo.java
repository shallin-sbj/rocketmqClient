package com.scl.rocketmq.demo;

import com.scl.rocketmq.config.RocketmqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class ProducerDemo {
    @Autowired
    private DefaultMQProducer defaultProducer;

    @Autowired
    private TransactionMQProducer transactionProducer;

    @Autowired
    private RocketmqProperties properties;

    @Autowired
    private DefaultMQPullConsumer defaultMQPullConsumer;

    private int i = 0;

    @RequestMapping(value = "/sendMsg", method = RequestMethod.GET)
    public void sendMsg() {
        for (int j = 0; j < 5000; j++) {
            Message msg = new Message("TopicTest1", // topic
                    "TagA", // tag
                    "OrderID00" + j, // key
                    ("Hello zebra mq" + j).getBytes());// body
            try {
                defaultProducer.send(msg, new SendCallback() {

                    @Override
                    public void onSuccess(SendResult sendResult) {
//                        System.out.println(sendResult);
                        log.info("msg: status " + sendResult.getSendStatus() + "info :" + new String(msg.getBody()));
                        // TODO 发送成功处理
                    }

                    @Override
                    public void onException(Throwable e) {
                        System.out.println(e);
                    }
                });
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping(value = "/sendTransactionMsg", method = RequestMethod.GET)
    public String sendTransactionMsg() {
        SendResult sendResult = null;
        for (int j = 0; j < 200; j++) {
            try {
                // 构造消息
                Message msg = new Message("TopicTest1", // topic
                        "TagA", // tag
                        "OrderID001", // key
                        ("Hello zebra mq" + j).getBytes());// body

                sendResult = transactionProducer.sendMessageInTransaction(msg, null);

                log.info("msg: status " + sendResult.getSendStatus() + "  info :" + new String(msg.getBody()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sendResult.toString();
    }

    @RequestMapping(value = "/sendMsgOrder", method = RequestMethod.GET)
    public void sendMsgOrder() {
        for (int j = 0; j < 5000; j++) {
            Message msg = new Message("TopicTest1", // topic
                    "TagA", // tag
                    "OrderID00" + j, // key
                    ("Hello zebra mq" + j).getBytes());// body
            try {
                defaultProducer.send(msg, new MessageQueueSelector() {
                    @Override
                    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                        System.out.println("Order send Message ;" + new String(msg.getBody()));
                        int index = ((Integer) arg) % mqs.size();
                        return mqs.get(index);
                    }
                }, i);// i==arg
                i++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
