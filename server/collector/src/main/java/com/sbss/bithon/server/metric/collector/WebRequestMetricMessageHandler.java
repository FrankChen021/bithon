package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebRequestMetricMessage;
import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.collector.GenericMessage;
import com.sbss.bithon.server.common.service.UriNormalizer;
import com.sbss.bithon.server.meta.EndPointLink;
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
public class WebRequestMetricMessageHandler extends AbstractMetricMessageHandler {

    private final UriNormalizer uriNormalizer;

    public WebRequestMetricMessageHandler(UriNormalizer uriNormalizer,
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
    void toMetricObject(GenericMessage message) {
        if (message.getLong("requestCount") <= 0 ) {
            return;
        }

        UriNormalizer.NormalizedResult result = uriNormalizer.normalize(message.getApplicationName(),
                                                                        message.getString("uri"));
        if (result.getUri() == null) {
            return;
        }
        message.set("uri", result.getUri());

        String srcApplication;
        EndPointType srcEndPointType;
        if (StringUtils.isEmpty(message.getString("srcApplication"))) {
            srcApplication = "Bithon-Unknown";
            srcEndPointType = EndPointType.UNKNOWN;
        } else {
            srcApplication = message.getString("srcApplication");
            srcEndPointType = EndPointType.APPLICATION;
        }
        message.set("endpoint", new EndPointLink(srcEndPointType,
                                srcApplication,
                                EndPointType.APPLICATION,
                                message.getApplicationName()));
    }
}
