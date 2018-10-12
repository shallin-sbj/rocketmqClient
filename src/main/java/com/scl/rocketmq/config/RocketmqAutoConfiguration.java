package com.scl.rocketmq.config;

import com.scl.rocketmq.transaction.TransactionListenerImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.*;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(RocketmqProperties.class)
@ConditionalOnProperty(prefix = RocketmqProperties.PREFIX, value = "namesrvAddr")
@Slf4j
public class RocketmqAutoConfiguration {

    private static final Logger log = LogManager.getLogger(RocketmqAutoConfiguration.class);

    @Autowired
    private RocketmqProperties properties;

    @Autowired
    private ApplicationEventPublisher publisher;

    private static boolean isFirstSub = true;

    private static long startTime = System.currentTimeMillis();

    /**
     * 初始化向rocketmq发送普通消息的生产者
     */
    @Bean
    @ConditionalOnProperty(prefix = RocketmqProperties.PREFIX, value = "producerInstanceName")
    public DefaultMQProducer defaultProducer() throws MQClientException {
        /**
         * 一个应用创建一个Producer，由应用来维护此对象，可以设置为全局对象或者单例
         * 注意：ProducerGroupName需要由应用来保证唯一
         * ProducerGroup这个概念发送普通的消息时，作用不大，但是发送分布式事务消息时，比较关键，
         * 因为服务器会回查这个Group下的任意一个Producer
         */
        DefaultMQProducer producer = new DefaultMQProducer(properties.getProducerGroupName());
        producer.setNamesrvAddr(properties.getNamesrvAddr());
        producer.setInstanceName(properties.getProducerInstanceName());
        producer.setVipChannelEnabled(false);
        producer.setRetryTimesWhenSendAsyncFailed(10);
        /**
         * Producer对象在使用之前必须要调用start初始化，初始化一次即可<br>
         * 注意：切记不可以在每次发送消息时，都调用start方法
         */
        producer.start();
        log.info("RocketMq defaultProducer Started.");
        return producer;
    }

    /**
     * 初始化向rocketmq发送事务消息的生产者
     */
    @Bean
    @ConditionalOnProperty(prefix = RocketmqProperties.PREFIX, value = "producerTranInstanceName")
    public TransactionMQProducer transactionProducer() throws MQClientException {
        /**
         * 一个应用创建一个Producer，由应用来维护此对象，可以设置为全局对象或者单例<br>
         * 注意：ProducerGroupName需要由应用来保证唯一<br>
         * ProducerGroup这个概念发送普通的消息时，作用不大，但是发送分布式事务消息时，比较关键，
         * 因为服务器会回查这个Group下的任意一个Producer
         */
        TransactionMQProducer producer = new TransactionMQProducer(properties.getTransactionProducerGroupName());
        TransactionListener transactionListener = new TransactionListenerImpl();
        producer.setNamesrvAddr(properties.getNamesrvAddr());
        producer.setInstanceName(properties.getProducerTranInstanceName());
        producer.setRetryTimesWhenSendAsyncFailed(10);

        // 事务回查最小并发数
        producer.setCheckThreadPoolMinSize(2);
        // 事务回查最大并发数
        producer.setCheckThreadPoolMaxSize(2);
        // 队列数
        producer.setCheckRequestHoldMax(2000);

        ExecutorService executorService = new ThreadPoolExecutor(2, 5, 100, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("client-transaction-msg-check-thread");
                return thread;
            }
        });

        producer.setExecutorService(executorService);
        producer.setTransactionListener(transactionListener);
        /**
         * Producer对象在使用之前必须要调用start初始化，初始化一次即可<br>
         * 注意：切记不可以在每次发送消息时，都调用start方法
         */
        producer.start();
        log.info("RocketMq TransactionMQProducer Started.");
        return producer;
    }

    /**
     * 初始化rocketmq消息监听方式的消费者
     */
    @Bean
    @ConditionalOnProperty(prefix = RocketmqProperties.PREFIX, value = "enablePullConsumer", havingValue = "false")
    public DefaultMQPushConsumer pushConsumer() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(properties.getConsumerGroupName());
        consumer.setNamesrvAddr(properties.getNamesrvAddr());
        consumer.setInstanceName(properties.getConsumerInstanceName());
        if (properties.isConsumerBroadcasting()) {
            consumer.setMessageModel(MessageModel.BROADCASTING);
        }
        consumer.setConsumeMessageBatchMaxSize(
                properties.getConsumerBatchMaxSize() == 0 ? 1 : properties.getConsumerBatchMaxSize());// 设置批量消费，以提升消费吞吐量，默认是1
        /**
         * 订阅指定topic下tags
         */
        List<String> subscribeList = properties.getSubscribe();
        for (String sunscribe : subscribeList) {
            consumer.subscribe(sunscribe.split(":")[0], sunscribe.split(":")[1]);
        }
        if (properties.isEnableOrderConsumer()) {
            consumer.registerMessageListener((List<MessageExt> msgs, ConsumeOrderlyContext context) -> {
                try {
                    context.setAutoCommit(true);
                    msgs = filter(msgs);
                    if (msgs.size() == 0) {
                        return ConsumeOrderlyStatus.SUCCESS;
                    }
                    this.publisher.publishEvent(new RocketmqEvent(msgs, consumer));
                } catch (Exception e) {
                    e.printStackTrace();
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
                return ConsumeOrderlyStatus.SUCCESS;
            });
        } else {
            consumer.registerMessageListener((List<MessageExt> msgs, ConsumeConcurrentlyContext context) -> {
                try {
                    msgs = filter(msgs);
                    if (msgs.size() == 0) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    this.publisher.publishEvent(new RocketmqEvent(msgs, consumer));
                } catch (Exception e) {
                    e.printStackTrace();
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 延迟5秒再启动，主要是等待spring事件监听相关程序初始化完成，否则，回出现对RocketMQ的消息进行消费后立即发布消息到达的事件，然而此事件的监听程序还未初始化，从而造成消息的丢失
                    Thread.sleep(5000);
                    try {
                        consumer.start();
                    } catch (Exception e) {
                        log.info("RocketMq pushConsumer Start failure!!!.");
                        log.error(e.getMessage(), e);
                    }
                    log.info("RocketMq pushConsumer Started.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }).start();

        return consumer;
    }

    private List<MessageExt> filter(List<MessageExt> msgs) {
        if (isFirstSub && !properties.isEnableHisConsumer()) {
            msgs = msgs.stream().filter(item -> startTime - item.getBornTimestamp() < 0).collect(Collectors.toList());
        }
        if (isFirstSub && msgs.size() > 0) {
            isFirstSub = false;
        }
        return msgs;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * 初始化rocketmq消息监听方式的消费者  拉的方式
     *
     * @return
     * @throws MQClientException
     */
    @Bean
    @ConditionalOnProperty(prefix = RocketmqProperties.PREFIX, value = "enablePullConsumer", havingValue = "true")
    public DefaultMQPullConsumer pullConsumer() throws MQClientException {
        final MQPullConsumerScheduleService scheduleService = new MQPullConsumerScheduleService(properties.getConsumerGroupName());
        DefaultMQPullConsumer pullConsumer = scheduleService.getDefaultMQPullConsumer();
        pullConsumer.setNamesrvAddr(properties.getNamesrvAddr());
        scheduleService.setMessageModel(MessageModel.CLUSTERING);
        /**
         * 订阅指定topic下tags
         */
        List<String> subscribeList = properties.getSubscribe();
        for (String sunscribe : subscribeList) {
            scheduleService.registerPullTaskCallback(sunscribe.split(":")[0], new PullTaskCallback() {
                @Override
                public void doPullTask(MessageQueue mq, PullTaskContext context) {
                    MQPullConsumer consumer = context.getPullConsumer();
                    try {
                        // 获取从哪里拉取
                        long offset = consumer.fetchConsumeOffset(mq, false);
                        if (offset < 0) {
                            offset = 0;
                        }
                        PullResult pullResult = consumer.pull(mq, "*", offset, 32);
                        if (!CollectionUtils.isEmpty(pullResult.getMsgFoundList())) {
                            switch (pullResult.getPullStatus()) {
                                case FOUND:
                                    List<MessageExt> messageExtList = pullResult.getMsgFoundList();
                                    for (MessageExt m : messageExtList) {
                                        log.info("pullConsumer 并消费了：" + new String(m.getBody()));
                                    }
                                    break;
                                case NO_MATCHED_MSG:
                                    break;
                                case NO_NEW_MSG:
                                case OFFSET_ILLEGAL:
                                    break;
                                default:
                                    break;
                            }
                            // 存储Offset，客户端每隔5s会定时刷新到Broker
                            consumer.updateConsumeOffset(mq, pullResult.getNextBeginOffset());
                            // 设置再过100ms后重新拉取
                            context.setPullNextDelayTimeMillis(100);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            scheduleService.start();
        }
        return pullConsumer;
    }
}