package com.sbss.bithon.collector.meta;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 9:50 上午
 */
public class Metadata {

    @Getter
    @Setter
    private Long id;

    @Getter
    private final String name;

    @Getter
    private final MetadataType type;

    @Getter
    private final Long parent;

    public Metadata(String name, MetadataType type, Long parent) {
        this.name = name;
        this.type = type;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof Metadata) {
            Metadata that = ((Metadata) rhs);
            return this.name.equals(that.name)
                && this.type.equals(that.type)
                && this.parent.equals(that.parent);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, parent);
    }
}
