package com.sbss.bithon.collector.meta.jdbc;

import com.sbss.bithon.collector.common.pojo.DisplayableText;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.collector.meta.EndPointLink;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.Metadata;
import com.sbss.bithon.collector.meta.MetadataType;
import com.sbss.bithon.component.db.dao.MetadataDAO;
import com.sbss.bithon.component.db.jooq.tables.records.BithonMetadataRecord;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.stream.Collectors;

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
        return metadataDao.getMetadata(type.getType())
            .stream().map(r->new Metadata(r.getName(), r.getType(), r.getParentId())).collect(Collectors.toList());
    }

    @Override
    public long createMetricDimension(String dataSource,
                                      String dimensionName,
                                      String dimensionValue,
                                      long timestamp) {
        return metadataDao.upsertDimension(dataSource, dimensionName, dimensionValue, timestamp);
    }

    @Override
    public long createTopo(EndPointLink link) {
        return metadataDao.upsertTopo(link.getSrcEndPointType(),
                                      link.getSrcEndpoint(),
                                      link.getDstEndpointType(),
                                      link.getDstEndpoint());
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        BithonMetadataRecord application = metadataDao.getMetaByNameAndType(instanceName,
                                                                            MetadataType.APP_INSTANCE.getType());
        return application == null ? null : application.getName();
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        BithonMetadataRecord application = metadataDao.getMetaByNameAndType(applicationName,
                                                                            MetadataType.APPLICATION.getType());
        return application != null;
    }

    @Override
    public Collection<DisplayableText> getMetricDimensions(String dataSourceName,
                                                           String dimensionName,
                                                           String startISO8601,
                                                           String endISO8601) {
        try {
            return metadataDao.getMetricDimensions(dataSourceName,
                                                   dimensionName,
                                                   DateTimeUtils.fromISO8601(startISO8601),
                                                   DateTimeUtils.fromISO8601(endISO8601))
                .stream().map(r -> new DisplayableText(r.getId().toString(),
                                                       r.getDimensionValue())).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
