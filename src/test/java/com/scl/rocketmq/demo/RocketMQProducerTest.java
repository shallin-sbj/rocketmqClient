package com.scl.rocketmq.demo;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.common.message.Message;

import java.util.UUID;

public class RocketMQProducerTest {
    public static void main(String[] args) {

        String mqTopics = "MAC_COLLECT2";
        String producerMqGroupName = "MacProducer";
//        String mqNameServer = "192.168.101.213:9876";
//        String mqNameServer = "192.168.154.130:9876;192.168.154.131:9876;192.168.154.132:9876;192.168.154.133:9876";
        String mqNameServer = "10.31.53.94:9876;10.80.64.65:9876;10.81.83.178:9876;10.80.114.17:9876";
        RocketMQProducer mqProducer = new RocketMQProducer(mqNameServer, producerMqGroupName, mqTopics);
        mqProducer.init();
        for (int i = 0; i < 100; i++) {
            Message message = new Message();
            DeviceMacInfo info = new DeviceMacInfo();
            info.setMac(i+"");

            info.setUniquekey(UUID.randomUUID().toString().substring(0,32));
            String infoStr = JSON.toJSONString(info);
            message.setBody(infoStr.getBytes());
            message.setTags("mac");
//            mqProducer.send(message);
            System.out.println(info.toString());
            mqProducer.send(message, new MessageQueueSelectorForOrderId(), i);
        }
    }
}
