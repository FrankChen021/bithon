package com.sbss.bithon.server.metric.dimension;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29
 */
public interface IDimensionSpecVisitor<T> {
    T visit(LongDimensionSpec spec);
    T visit(StringDimensionSpec spec);
}
