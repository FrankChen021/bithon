package com.sbss.bithon.server.meta.storage;

import com.sbss.bithon.server.common.pojo.DisplayableText;
import com.sbss.bithon.server.meta.EndPointLink;
import com.sbss.bithon.server.meta.Metadata;
import com.sbss.bithon.server.meta.MetadataType;

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

    long createTopo(EndPointLink link);

    /**
     * @param instanceName host+port
     */
    String getApplicationByInstance(String instanceName);

    boolean isApplicationExist(String applicationName);

    Collection<DisplayableText> getMetricDimensions(String dataSourceName,
                                                    String dimensionName,
                                                    String startISO8601,
                                                    String endISO8601);
}
