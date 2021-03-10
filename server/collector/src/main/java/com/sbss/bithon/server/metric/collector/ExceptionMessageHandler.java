package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ExceptionMetricMessage;
import com.sbss.bithon.server.collector.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.server.common.utils.ReflectionUtils;
import com.sbss.bithon.server.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.server.meta.MetadataType;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 9:31 下午
 */
@Slf4j
@Service
public class ExceptionMessageHandler extends AbstractThreadPoolMessageHandler<MessageHeader, ExceptionMetricMessage> {

    private final IMetaStorage metaStorage;

    ExceptionMessageHandler(IMetaStorage metaStorage) {
        super(1, 5, Duration.ofMinutes(3), 4096);
        this.metaStorage = metaStorage;
    }

    @Override
    protected void onMessage(MessageHeader header, ExceptionMetricMessage body) {
        long appId = metaStorage.getOrCreateMetadataId(header.getAppName(), MetadataType.APPLICATION, 0L);
        long instanceId = metaStorage.getOrCreateMetadataId(header.getHostName(), MetadataType.APP_INSTANCE, appId);

        Map<String, Object> metrics = ReflectionUtils.getFields(body);
        metrics.put("appName", header.getAppName());
        metrics.put("instanceName", header.getHostName());
        metrics.put("appId", appId);
        metrics.put("instanceId", instanceId);
        metrics.put("interval", body.getInterval());
        metrics.put("timestamp", DateTimeUtils.dropMilliseconds(body.getTimestamp()));
    }
}
