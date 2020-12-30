package com.sbss.bithon.collector.meta.api;

import com.sbss.bithon.collector.meta.MetadataType;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 3:41 下午
 */
@Data
public class GetMetadataListRequest {
    @NotNull
    private MetadataType type;
}
