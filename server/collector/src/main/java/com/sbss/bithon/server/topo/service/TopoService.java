package com.sbss.bithon.server.topo.service;

import com.sbss.bithon.component.db.dao.MetadataDAO;
import com.sbss.bithon.server.common.utils.datetime.TimeSpan;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:41
 */
@Service
public class TopoService {

    // TODO: topo仍然使用metric存储，而不使用meta进行存储
    // 一分钟为单位进行聚合存储
    private MetadataDAO metadataDAO;


    public Topo getCallee(TimeSpan startTime,
                          TimeSpan endTime,
                          String application,
                          int calleeDepth) {
        return null;
    }

    public Topo getCaller(String startTimeISO8601,
                          String endTimeISO8601,
                          String application,
                          @Min(1) @Max(5) int callerDepth) {
        return null;
    }
}
