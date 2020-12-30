package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:52 下午
 */
public class MemoryEntity implements Serializable {

    // 分配给应用的总内存数（单位：字节）
    private long allocatedBytes;

    // 当前空闲的内存数（单位：字节）
    private long freeBytes;
}
