package com.sbss.bithon.agent.core.metrics.jvm;

import java.io.Serializable;

/**
 * @author: frank.chen021@outlook.com
 * @date: 2020/12/29 9:58 下午
 */
public class MetaspaceMetric implements Serializable {
    /**
     * allocated metaspace in bytes
     */
    public long metaspaceCommittedBytes = -1;
    public long metaspaceUsedBytes = -1;

}
