package org.bithon.server.storage.datasource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.bithon.server.storage.datasource.filter.IInputRowFilter;
import org.bithon.server.storage.datasource.flatten.IFlattener;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.transformer.ITransformer;

import java.util.List;

/**
 * @author Frank Chen
 * @date 11/4/22 10:49 PM
 */
@Data
public class TransformSpec {
    private final List<IFlattener> flatteners;
    private final List<IInputRowFilter> prefilters;
    private final List<ITransformer> transformers;
    private final List<IInputRowFilter> postfilters;

    @JsonCreator
    public TransformSpec(@JsonProperty("prefilters") List<IInputRowFilter> prefilters,
                         @JsonProperty("flatteners") List<IFlattener> flatteners,
                         @JsonProperty("transformers") List<ITransformer> transformers,
                         @JsonProperty("postfilters") List<IInputRowFilter> postfilters) {
        this.flatteners = flatteners;
        this.prefilters = prefilters;
        this.transformers = transformers;
        this.postfilters = postfilters;
    }

    /**
     * @return a boolean value, whether to include this row in result set
     */
    public boolean transform(IInputRow inputRow) {
        if (prefilters != null) {
            for (IInputRowFilter filter : prefilters) {
                if (!filter.shouldInclude(inputRow)) {
                    return false;
                }
            }
        }
        if (flatteners != null) {
            for (IFlattener flattener : flatteners) {
                flattener.flatten(inputRow);
            }
        }
        if (transformers != null) {
            for (ITransformer transformer : transformers) {
                transformer.transform(inputRow);
            }
        }
        if (postfilters != null) {
            for (IInputRowFilter filter : postfilters) {
                if (!filter.shouldInclude(inputRow)) {
                    return false;
                }
            }
        }
        return true;
    }
}
