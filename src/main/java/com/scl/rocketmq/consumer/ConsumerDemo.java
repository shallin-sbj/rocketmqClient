package com.scl.rocketmq.consumer;

import com.scl.rocketmq.config.RocketmqEvent;
import com.scl.rocketmq.service.MessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class ConsumerDemo {

    @Autowired
    @Qualifier("messageProcessorImpl")
    private MessageProcessor messageProcessor;

    /**
     * condition 指定消费那个 topic 以及那个tags
     *
     * @param event
     */
    @EventListener(condition = "#event.msgs[0].topic=='TopicTest1' && #event.msgs[0].tags=='TagA'")
    public void rocketmqMsgListen(RocketmqEvent event) {
        try {
            // TODO 进行业务逻辑处理
            if (event != null && !CollectionUtils.isEmpty(event.getMsgs())) {
                messageProcessor.handleMessageList(event.getMsgs());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
