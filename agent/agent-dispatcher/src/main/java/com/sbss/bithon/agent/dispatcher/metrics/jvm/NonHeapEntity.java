package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:54 下午
 */
public class NonHeapEntity implements Serializable {
    // 约等于XX:MaxPermSize的值（单位：字节）
    private long nonHeapBytes;
    // 约等于-XX:PermSize的值（单位：字节）
    private long nonHeapInitBytes;
    // 已经被使用的内存大小（单位：字节）
    private long nonHeapUsedBytes;
    // 当前可使用的内存大小，包括used（单位：字节）
    private long nonHeapCommitted;
}
