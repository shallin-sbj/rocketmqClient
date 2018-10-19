package com.scl.rocketmq.demo;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户终端信息表
 */
@Setter
@Getter
public class DeviceMacInfo {

    /**
     * MD5摘要
     */
    private String uniquekey;

    /**
     * 移动端设备标识
     */
    private String uid;

    /**
     * MAC地址
     */
    private String mac;
    /**
     * 客户IP地址
     */
    private String ip;
    /**
     * 来源渠道
     */
    private String channel;
    /**
     * sdk版本
     */
    private String sdkver;
    /**
     * 上传日期
     */
    private Integer dt;
    /**
     * 创建时间
     */
    private long createtime;


    @Override
    public String toString() {
        return "DeviceMacInfo{" +
                "uniquekey='" + uniquekey + '\'' +
                ", uid='" + uid + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}