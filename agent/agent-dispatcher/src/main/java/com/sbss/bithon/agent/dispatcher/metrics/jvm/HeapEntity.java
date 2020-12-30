package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:52 下午
 */
public class HeapEntity implements Serializable {
    // 约等于-Xmx的值（单位：字节）
    private long heapBytes;
    // 约等于-Xms的值（单位：字节）
    private long heapInitBytes;
    // 已经被使用的内存大小（单位：字节）
    private long heapUsedBytes;
    // 当前可使用的内存大小，包括used（单位：字节）
    private long heapCommittedBytes;
}
