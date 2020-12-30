package com.sbss.bithon.collector.tracing.storage;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/4 8:27 下午
 */
public interface ITraceStorage {
    ITraceWriter createWriter();

    ITraceReader createReader();
}
