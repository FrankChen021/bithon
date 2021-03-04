package com.sbss.bithon.collector.meta.jdbc;

import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.Metadata;
import com.sbss.bithon.collector.meta.MetadataType;
import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.component.db.dao.MetadataDAO;
import com.sbss.bithon.component.db.jooq.tables.records.BithonMetadataRecord;
import org.jooq.DSLContext;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:56 下午
 */
public class MetadataJdbcStorage implements IMetaStorage {

    private final MetadataDAO metadataDao;

    public MetadataJdbcStorage(DSLContext dsl) {
        this.metadataDao = new MetadataDAO(dsl);
    }

    @Override
    public long getOrCreateMetadataId(String name, MetadataType type, long parent) {
        return metadataDao.upsertMetadata(name, type.getType(), parent);
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        return null;
    }

    @Override
    public long createMetricDimension(String dataSource,
                                      String dimensionName,
                                      String dimensionValue,
                                      long timestamp) {
        return metadataDao.upsertDimension(dataSource, dimensionName, dimensionValue, timestamp);
    }

    @Override
    public long createTopo(EndPointType srcEndpointType,
                           String srcEndpoint,
                           EndPointType dstEndpointType,
                           String dstEndpoint) {
        return metadataDao.upsertTopo(srcEndpointType,
                                      srcEndpoint,
                                      dstEndpointType,
                                      dstEndpoint);
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        BithonMetadataRecord application = metadataDao.getMetaByNameAndType(instanceName,
                                                                            MetadataType.INSTANCE.getType());
        return application == null ? null : application.getName();
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        BithonMetadataRecord application = metadataDao.getMetaByNameAndType(applicationName,
                                                                            MetadataType.APPLICATION.getType());
        return application != null;
    }
}
