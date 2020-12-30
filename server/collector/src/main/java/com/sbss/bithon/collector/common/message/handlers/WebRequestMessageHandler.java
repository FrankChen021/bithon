package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebRequestMessage;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.input.InputRow;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.IMetricWriter;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.MetadataType;
import com.sbss.bithon.collector.common.service.UriNormalizer;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
@Service
public class WebRequestMessageHandler extends AbstractThreadPoolMessageHandler<WebRequestMessage> {

    private final UriNormalizer uriNormalizer;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricWriter;

    public WebRequestMessageHandler(UriNormalizer uriNormalizer,
                                    IMetaStorage metaStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager,
                                    IMetricStorage metricStorage) throws IOException {
        super(2, 20, Duration.ofSeconds(60), 4096);
        this.uriNormalizer = uriNormalizer;
        this.metaStorage = metaStorage;
        this.metricWriter = metricStorage.createMetricWriter(dataSourceSchemaManager.loadFromResource("web-request-metrics"));
    }

    @Override
    protected void onMessage(WebRequestMessage message) throws IOException {
        if ( message.getRequestEntity().getRequestCount() <= 0 ) {
            return;
        }

        Map<String, Object> metrics = ReflectionUtils.getFields(message.getRequestEntity());

        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        UriNormalizer.NormalizedResult result = uriNormalizer.normalize(message.getAppName(), message.getRequestEntity().getUri());
        if (result.getUri() == null) {
            return;
        }
        long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0);
        long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);
        long uriId = metaStorage.getOrCreateMetadataId(result.getUri(), MetadataType.URI, appId);

        metrics.put("appId", appId);
        metrics.put("appName", appName);
        metrics.put("instanceId", instanceId);
        metrics.put("instanceName", instanceName);
        metrics.put("interval", message.getInterval());
        metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));
        metrics.put("uriId", uriId);
        metrics.put("uri", result.getUri());

        this.metricWriter.write(new InputRow(metrics));
    }
}
