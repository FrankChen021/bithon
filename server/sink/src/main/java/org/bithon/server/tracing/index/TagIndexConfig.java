package org.bithon.server.tracing.index;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 3/3/22 11:54 AM
 */
@Data
public class TagIndexConfig {
    /**
     * for which tags we're building indexes.
     *
     * Currently, we define a list of tag names to make this index module work in minimal work.
     *
     * Since the names may be the same in different span logs, if we only want to build indexes for some specific span logs,
     * a filter for span is needed. That might be the future work if necessary.
     */
    private List<String> names = Collections.emptyList();
}
