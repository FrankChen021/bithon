package com.sbss.bithon.agent.dispatcher.metrics.jvm;

import java.io.Serializable;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:58 下午
 */
public class MetaspaceEntity implements Serializable {
    // metaspace已分配的内存大小（单位：字节）
    public long metaspaceCommittedBytes;

    // metaspace已使用的内存大小（单位：字节）
    public long metaspaceUsedBytes;

}
