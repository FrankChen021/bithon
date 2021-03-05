package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebRequestMessage;
import com.sbss.bithon.collector.common.service.UriNormalizer;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.component.db.dao.EndPointType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
@Service
public class WebRequestMessageHandler extends AbstractMetricMessageHandler<WebRequestMessage> {

    private final UriNormalizer uriNormalizer;

    public WebRequestMessageHandler(UriNormalizer uriNormalizer,
                                    IMetaStorage metaStorage,
                                    IMetricStorage metricStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("web-request-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2, 20, Duration.ofSeconds(60), 4096);
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    SizedIterator toIterator(WebRequestMessage message) {
        if (message.getRequestEntity().getRequestCount() <= 0) {
            return null;
        }

        String appName = message.getAppName();
        String instanceName = message.getHostName() + ":" + message.getPort();

        return new SizedIterator() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public GenericMetricObject next() {
                UriNormalizer.NormalizedResult result = uriNormalizer.normalize(message.getAppName(), message.getRequestEntity().getUri());
                if (result.getUri() == null) {
                    return null;
                }

                GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                                      appName,
                                                                      instanceName);
                metrics.put("interval", message.getInterval());
                metrics.put("uri", result.getUri());
                ReflectionUtils.getFields(message.getRequestEntity(), metrics);

                String srcApplication;
                EndPointType srcEndPointType;
                if (StringUtils.isEmpty(message.getRequestEntity().getSrcApplication())) {
                    srcApplication = "Bithon-Unknown";
                    srcEndPointType = EndPointType.UNKNOWN;
                } else {
                    srcApplication = message.getRequestEntity().getSrcApplication();
                    srcEndPointType = EndPointType.APPLICATION;
                }
                metrics.setEndpointLink(srcEndPointType,
                                        srcApplication,
                                        EndPointType.APPLICATION,
                                        message.getAppName());

                return metrics;
            }
        };
    }
}
