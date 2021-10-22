/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.meta.api;

import org.bithon.server.common.pojo.DisplayableText;
import org.bithon.server.meta.Metadata;
import org.bithon.server.meta.storage.IMetaStorage;
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
