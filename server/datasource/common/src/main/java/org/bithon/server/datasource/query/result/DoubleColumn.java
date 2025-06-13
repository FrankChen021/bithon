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

package org.bithon.server.datasource.query.result;


import org.bithon.component.commons.expression.IDataType;

import java.util.BitSet;

/**
 * @author frank.chen021@outlook.com
 * @date 6/5/25 10:28 am
 */
public class DoubleColumn implements Column {
    private double[] data;
    private int size;
    private final String name;

    public static DoubleColumn of(String name, double... data) {
        return new DoubleColumn(name, data);
    }

    public DoubleColumn(String name, int capacity) {
        this.data = new double[capacity];
        this.size = 0;
        this.name = name;
    }

    public DoubleColumn(String name, double[] data) {
        this.data = data;
        this.size = data.length;
        this.name = name;
    }

    public DoubleColumn(String name, double[] data, int size) {
        this.data = data;
        this.size = size;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IDataType getDataType() {
        return IDataType.DOUBLE;
    }

    public void addObject(Object value) {
        if (value instanceof Number) {
            addInternal(((Number) value).doubleValue());
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
        addInternal(value);
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
        return (long) data[row];
    }

    @Override
    public Object getObject(int row) {
        return data[row];
    }

    public void set(int index, Double value) {
        data[index] = value;
    }

    public int size() {
        return size;
    }

    @Override
    public Column filter(BitSet keep) {
        DoubleColumn filtered = new DoubleColumn(this.name, keep.cardinality());
        for (int i = 0; i < this.size; i++) {
            if (keep.get(i)) {
                filtered.addDouble(this.data[i]);
            }
        }
        return filtered;
    }

    public double[] getData() {
        return data;
    }

    @Override
    public Column view(int[] selections, int length) {
        return new DoubleColumnView(this, selections, length);
    }

    private static class DoubleColumnView extends DoubleColumn {
        private final DoubleColumn delegate;
        private final int[] selections;
        private final int length;

        public DoubleColumnView(DoubleColumn delegate, int[] selections, int length) {
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
            DoubleColumn filtered = new DoubleColumn(this.getName(), keep.cardinality());
            for (int i = 0; i < this.length; i++) {
                if (keep.get(i)) {
                    filtered.addDouble(delegate.getDouble(selections[i]));
                }
            }
            return filtered;
        }

        @Override
        public int size() {
            return this.length;
        }
    }

    private void addInternal(double value) {
        if (size >= data.length) {
            double[] newData = new double[(data.length * 3 / 2)];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
        data[size++] = value;
    }
}
