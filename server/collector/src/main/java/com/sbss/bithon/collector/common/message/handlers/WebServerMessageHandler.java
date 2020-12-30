package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebServerMessage;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.input.InputRow;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.IMetricWriter;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.MetadataType;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 11:06 下午
 */
@Slf4j
@Service
public class WebServerMessageHandler extends AbstractThreadPoolMessageHandler<WebServerMessage> {

    private final IMetaStorage metaStorage;
    private final IMetricWriter metricWriter;

    public WebServerMessageHandler(IMetaStorage metaStorage,
                                   DataSourceSchemaManager dataSourceSchemaManager,
                                   IMetricStorage metricStorage) throws IOException {
        super(5, 10, Duration.ofMinutes(1), 1024);
        this.metaStorage = metaStorage;
        this.metricWriter = metricStorage.createMetricWriter(dataSourceSchemaManager.loadFromResource("web-server-metrics"));
    }

    @Override
    protected void onMessage(WebServerMessage message) throws IOException {
        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0L);
        long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("appId", appId);
        metrics.put("appName", appName);
        metrics.put("instanceId", instanceId);
        metrics.put("instanceName", instanceName);
        metrics.put("interval", message.getInterval());
        metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));

        ReflectionUtils.getFields(message.getServerEntity(), metrics);

        this.metricWriter.write(new InputRow(metrics));
    }
}
