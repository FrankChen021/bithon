package com.sbss.bithon.collector.tracing.api;

import com.sbss.bithon.collector.tracing.storage.TraceSpan;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:31 下午
 */
@Data
public class TraceSpanBo extends TraceSpan {

    public List<TraceSpanBo> children = new ArrayList<>();
}
