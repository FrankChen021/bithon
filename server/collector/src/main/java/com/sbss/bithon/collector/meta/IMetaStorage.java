package com.sbss.bithon.collector.meta;

import com.sbss.bithon.component.db.dao.EndPointType;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 9:49 上午
 */
public interface IMetaStorage {

    long getOrCreateMetadataId(String name, MetadataType type, long parent);

    Collection<Metadata> getMetadataByType(MetadataType type);

    long createMetricDimension(String dataSource,
                               String dimensionName,
                               String dimensionValue, long timestamp);

    long createTopo(EndPointType srcEndpointType,
                    String srcEndpoint,
                    EndPointType dstEndpointType,
                    String dstEndpoint);

    /**
     * @param instanceName host+port
     */
    String getApplicationByInstance(String instanceName);

    boolean isApplicationExist(String applicationName);
}
