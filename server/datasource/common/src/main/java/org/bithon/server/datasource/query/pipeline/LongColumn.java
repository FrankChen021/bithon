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

package org.bithon.server.datasource.query.pipeline;


import org.bithon.component.commons.expression.IDataType;

import java.util.BitSet;

/**
 * @author frank.chen021@outlook.com
 * @date 6/5/25 10:28 am
 */
public class LongColumn implements Column {
    /**
     * declared as package level visibility for performance reason
     */
    private long[] data;
    private int size;
    private final String name;

    public LongColumn(String name, int capacity) {
        this.data = new long[capacity];
        this.size = 0;
        this.name = name;
    }

    public LongColumn(String name, long[] data) {
        this.data = data;
        this.size = data.length;
        this.name = name;
    }

    public LongColumn(String name, long[] data, int size) {
        this.data = data;
        this.size = size;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public IDataType getDataType() {
        return IDataType.LONG;
    }

    public void addObject(Object value) {
        if (value instanceof Number) {
            addInternal(((Number) value).longValue());
        } else {
            throw new IllegalArgumentException("Unsupported column type: " + value);
        }
    }

    @Override
    public void addInt(int value) {
        addInternal(value);
    }

    @Override
    public void addLong(long value) {
        addInternal(value);
    }

    @Override
    public void addDouble(double value) {
        addInternal((long) value);
    }

    @Override
    public double getDouble(int row) {
        return data[row];
    }

    @Override
    public int getInt(int row) {
        return (int) data[row];
    }

    @Override
    public long getLong(int row) {
        return data[row];
    }

    @Override
    public Object getObject(int row) {
        return data[row];
    }

    public int size() {
        return size;
    }

    @Override
    public Column filter(BitSet keep) {
        org.bithon.server.datasource.query.pipeline.LongColumn filtered = new org.bithon.server.datasource.query.pipeline.LongColumn(this.name, keep.cardinality());
        for (int i = 0; i < this.size; i++) {
            if (keep.get(i)) {
                filtered.addLong(this.data[i]);
            }
        }
        return filtered;
    }

    public long[] getData() {
        return data;
    }

    protected void addInternal(long value) {
        if (size >= data.length) {
            long[] newData = new long[(data.length * 3 / 2)];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
        data[size++] = value;
    }

    @Override
    public Column view(int[] selections, int length) {
        return new LongColumnView(this, selections, length);
    }

    private static class LongColumnView extends LongColumn {
        private final LongColumn delegate;
        private final int[] selections;
        private final int length;

        public LongColumnView(LongColumn delegate, int[] selections, int length) {
            super(delegate.getName(), delegate.data, delegate.size);
            this.delegate = delegate;
            this.selections = selections;
            this.length = length;
        }

        @Override
        public double getDouble(int row) {
            return delegate.getDouble(selections[row]);
        }

        @Override
        public int getInt(int row) {
            return delegate.getInt(selections[row]);
        }

        @Override
        public long getLong(int row) {
            return delegate.getLong(selections[row]);
        }

        @Override
        public Object getObject(int row) {
            return delegate.getObject(selections[row]);
        }

        @Override
        public Column filter(BitSet keep) {
            BitSet originalKeep = new BitSet(this.length);
            for (int i = 0; i < this.length; i++) {
                if (keep.get(i)) {
                    originalKeep.set(selections[i]);
                }
            }
            return delegate.filter(originalKeep);
        }

        @Override
        public int size() {
            return this.length;
        }
    }
}
