package com.scl.rocketmq.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ConfigurationProperties(RocketmqProperties.PREFIX)
public class RocketmqProperties {

    public static final String PREFIX = "xy.rocketmq";

    private String namesrvAddr;

    private String producerGroupName;

    private String transactionProducerGroupName;

    private String consumerGroupName;

    private String producerInstanceName;

    private String consumerInstanceName;

    private String producerTranInstanceName;

    private int consumerBatchMaxSize;

    private boolean consumerBroadcasting;

    private boolean enableHisConsumer;

    private boolean enableOrderConsumer;

    private List subscribe = new ArrayList<>();

}