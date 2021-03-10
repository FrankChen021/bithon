package com.sbss.bithon.server.meta.api;

import com.sbss.bithon.server.common.pojo.DisplayableText;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.meta.Metadata;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 3:38 下午
 */
@CrossOrigin
@RestController
public class MetadataApi {

    private final IMetaStorage metaStorage;

    public MetadataApi(IMetaStorage metaStorage) {
        this.metaStorage = metaStorage;
    }

    @PostMapping("/api/meta/getMetadataList")
    public Collection<Metadata> getMetadataList(@Valid @RequestBody GetMetadataListRequest request) {
        return metaStorage.getMetadataByType(request.getType());
    }

    @PostMapping("/api/meta/dimensions")
    public Collection<DisplayableText> getDimensionValues(@Valid GetDimensionValueRequest request) {
        return metaStorage.getMetricDimensions(request.getDataSourceName(),
                                               request.getDimensionName(),
                                               request.getStartISO8601(),
                                               request.getEndISO8601());
    }
}
