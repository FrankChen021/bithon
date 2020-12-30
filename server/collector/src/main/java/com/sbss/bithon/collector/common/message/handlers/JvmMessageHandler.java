package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JvmMessage;
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
 * @date 2021/1/10 4:31 下午
 */
@Slf4j
@Service
public class JvmMessageHandler extends AbstractThreadPoolMessageHandler<JvmMessage> {

    private final IMetaStorage metaStorage;
    private final IMetricWriter metricStorageWriter;

    public JvmMessageHandler(IMetaStorage metaStorage,
                             DataSourceSchemaManager dataSourceSchemaManager,
                             IMetricStorage storage) throws IOException {
        super(5, 20, Duration.ofSeconds(60), 4096);

        this.metaStorage = metaStorage;
        this.metricStorageWriter = storage.createMetricWriter(dataSourceSchemaManager.loadFromResource("jvm-metrics"));
    }

    @Override
    protected void onMessage(JvmMessage message) throws IOException {
        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0L);
        long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("appName", appName);
        metrics.put("instanceName", instanceName);
        metrics.put("appId", appId);
        metrics.put("instanceId", instanceId);
        metrics.put("interval", message.getInterval());
        metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));

        ReflectionUtils.getFields(message.getClassesEntity(), metrics);
        ReflectionUtils.getFields(message.getCpuEntity(), metrics);
        ReflectionUtils.getFields(message.getHeapEntity(), metrics);
        ReflectionUtils.getFields(message.getNonHeapEntity(), metrics);
        ReflectionUtils.getFields(message.getMemoryEntity(), metrics);
        ReflectionUtils.getFields(message.getThreadEntity(), metrics);
        ReflectionUtils.getFields(message.getInstanceTimeEntity(), metrics);
        ReflectionUtils.getFields(message.getMetaspaceEntity(), metrics);

        this.metricStorageWriter.write(new InputRow(metrics));
    }
}
