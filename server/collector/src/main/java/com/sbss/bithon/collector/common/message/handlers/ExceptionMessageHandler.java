package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ExceptionMessage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.MetadataType;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 9:31 下午
 */
@Slf4j
@Service
public class ExceptionMessageHandler extends AbstractThreadPoolMessageHandler<ExceptionMessage> {

    private final IMetaStorage metaStorage;

    ExceptionMessageHandler(IMetaStorage metaStorage) {
        super(1, 5, Duration.ofMinutes(3), 4096);
        this.metaStorage = metaStorage;
    }

    @Override
    protected void onMessage(ExceptionMessage message) {
        if (CollectionUtils.isEmpty(message.getExceptionList())) {
            return;
        }

        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0L);
        long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);

        message.getExceptionList().forEach(exceptionEntity->{
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("appName", appName);
            metrics.put("instanceName", instanceName);
            metrics.put("appId", appId);
            metrics.put("instanceId", instanceId);
            metrics.put("interval", message.getInterval());
            metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));

            ReflectionUtils.getFields(exceptionEntity, metrics);

            log.debug("onExceptionMessage {}", metrics);
        });
    }
}
