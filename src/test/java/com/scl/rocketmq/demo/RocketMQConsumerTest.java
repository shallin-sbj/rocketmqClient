package com.scl.rocketmq.demo;

/**
 *
 */
public class RocketMQConsumerTest {

    public static void main(String[] args) {

//        String mqNameServer = "192.168.101.213:9876";
        String mqTopics = "MAC_COLLECT2";
        String producerMqGroupName = "producerGroupName";
//        String mqNameServer = "192.168.56.130:9876;192.168.56.132:9876";
        String mqNameServer = "10.31.53.94:9876;10.80.64.65:9876;10.81.83.178:9876;10.80.114.17:9876";
//        String mqNameServer = "192.168.154.130:9876;192.168.154.131:9876;192.168.154.132:9876;192.168.154.133:9876";
        RocketMQListener mqListener = new RocketMQListener();
        RocketMQConsumer mqConsumer = new RocketMQConsumer(mqListener, mqNameServer, producerMqGroupName, mqTopics);
        mqConsumer.init();
        try {
            Thread.sleep(1000 * 60L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
