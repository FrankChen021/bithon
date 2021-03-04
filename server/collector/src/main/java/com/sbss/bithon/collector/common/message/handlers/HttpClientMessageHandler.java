package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.HttpClientMessage;
import com.sbss.bithon.collector.common.service.UriNormalizer;
import com.sbss.bithon.collector.common.utils.NetworkUtils;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.component.db.dao.EndPointType;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
@Service
public class HttpClientMessageHandler extends AbstractMetricMessageHandler<HttpClientMessage> {

    private final UriNormalizer uriNormalizer;

    public HttpClientMessageHandler(UriNormalizer uriNormalizer,
                                    IMetaStorage metaStorage,
                                    IMetricStorage metricStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("http-client-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              20,
              Duration.ofSeconds(60), 4096);
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    SizedIterator toIterator(HttpClientMessage message) {
        if (message.getHttpClient().getRequestCount() <= 0) {
            return null;
        }

        String appName = message.getAppName() + "-" + message.getEnv();
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
                GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                                      appName,
                                                                      instanceName);
                metrics.put("interval", message.getInterval());
                ReflectionUtils.getFields(message.getHttpClient(), metrics);
                return metrics;
            }
        };
    }

    @Override
    protected boolean beforeProcessMetricObject(GenericMetricObject metricObject) throws Exception {
        URI uri = new URI(metricObject.getString("uri"));

        UriNormalizer.NormalizedResult result = uriNormalizer.normalize(metricObject.getApplicationName(),
                                                                        NetworkUtils.formatUri(uri));
        if (result.getUri() == null) {
            return false;
        }

        String targetHostPort = parseHostPort(uri.getHost(), uri.getPort());
        if (targetHostPort == null) {
            log.warn("TargetHost is blank. {}", metricObject);
            return false;
        }

        if (NetworkUtils.isIpAddress(uri.getHost())) {

            // try to get application info by instance name to see if its an internal application
            String targetApplicationName = getMetaStorage().getApplicationByInstance(targetHostPort);

            if (targetApplicationName != null) {
                metricObject.setTargetEndpoint(EndPointType.APPLICATION, targetApplicationName);
            } else {
                //
                // if the target application has not been in service yet,
                // it of course can't be found in the metadata storage
                //
                // TODO: This record should be fixed when a new instance is inserted into the metadata storage
                metricObject.setTargetEndpoint(EndPointType.WEB_SERVICE, targetHostPort);
            }
        } else {
            if (getMetaStorage().isApplicationExist(targetHostPort)) {
                metricObject.setTargetEndpoint(EndPointType.APPLICATION, targetHostPort);
            } else {
                metricObject.setTargetEndpoint(EndPointType.DOMAIN, targetHostPort);
            }
        }

        return true;
    }

    private String parseHostPort(String targetHost, int targetPort) {
        if (StringUtils.isBlank(targetHost)) {
            return null;
        }

        if (targetPort < 0) {
            return targetHost;
        } else {
            return targetHost + ":" + targetPort;
        }
    }
}
