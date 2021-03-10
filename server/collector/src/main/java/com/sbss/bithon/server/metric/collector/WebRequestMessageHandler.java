package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebRequestMetricMessage;
import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.common.service.UriNormalizer;
import com.sbss.bithon.server.common.utils.ReflectionUtils;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
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
public class WebRequestMessageHandler extends AbstractMetricMessageHandler<MessageHeader, WebRequestMetricMessage> {

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
    SizedIterator toIterator(MessageHeader header, WebRequestMetricMessage message) {
        if (message.getRequestCount() <= 0) {
            return null;
        }

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
                UriNormalizer.NormalizedResult result = uriNormalizer.normalize(header.getAppName(),
                                                                                message.getUri());
                if (result.getUri() == null) {
                    return null;
                }

                GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                                      header.getAppName(),
                                                                      header.getHostName());
                ReflectionUtils.getFields(message, metrics);
                metrics.put("uri", result.getUri());

                String srcApplication;
                EndPointType srcEndPointType;
                if (StringUtils.isEmpty(message.getSrcApplication())) {
                    srcApplication = "Bithon-Unknown";
                    srcEndPointType = EndPointType.UNKNOWN;
                } else {
                    srcApplication = message.getSrcApplication();
                    srcEndPointType = EndPointType.APPLICATION;
                }
                metrics.setEndpointLink(srcEndPointType,
                                        srcApplication,
                                        EndPointType.APPLICATION,
                                        header.getAppName());

                return metrics;
            }
        };
    }
}
