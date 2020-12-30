package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolMessage;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.input.InputRow;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.IMetricWriter;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.MetadataType;
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
public class ThreadPoolMessageHandler extends AbstractThreadPoolMessageHandler<ThreadPoolMessage> {

    private final IMetaStorage metaStorage;
    private final IMetricWriter metricStorageWriter;

    public ThreadPoolMessageHandler(IMetaStorage metaStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager,
                                    IMetricStorage storage) throws IOException {
        super(5, 20, Duration.ofSeconds(60), 4096);

        this.metaStorage = metaStorage;
        this.metricStorageWriter = storage.createMetricWriter(dataSourceSchemaManager.loadFromResource("thread-pool-metrics"));
    }

    @Override
    protected void onMessage(ThreadPoolMessage message) {
        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0L);
        long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);

        for(ThreadPoolEntity entity : message.getPools()) {
            this.execute(()->{
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("appName", appName);
                metrics.put("instanceName", instanceName);
                metrics.put("appId", appId);
                metrics.put("instanceId", instanceId);
                metrics.put("interval", message.getInterval());
                metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));

                ReflectionUtils.getFields(entity, metrics);

                try {
                    this.metricStorageWriter.write(new InputRow(metrics));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
