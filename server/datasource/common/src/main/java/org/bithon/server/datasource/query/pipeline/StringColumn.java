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
 * @date 6/5/25 10:29 am
 */
public class StringColumn implements Column {
    private String[] data;
    private int size;
    private final String name;

    public StringColumn(String name, int capacity) {
        this.data = new String[capacity];
        this.size = 0;
        this.name = name;
    }

    public StringColumn(String name, String[] data, int size) {
        this.data = data;
        this.size = size;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String get(int row) {
        return data[row];
    }

    @Override
    public IDataType getDataType() {
        return IDataType.STRING;
    }

    public void addObject(Object value) {
        if (value == null) {
            addInternal("");
        } else if (value instanceof String) {
            addInternal((String) value);
        } else {
            addInternal(value.toString());
        }
    }

    @Override
    public void addInt(int value) {
        addInternal(String.valueOf(value));
    }

    @Override
    public void addLong(long value) {
        addInternal(String.valueOf(value));
    }

    @Override
    public void addDouble(double value) {
        addInternal(String.valueOf(value));
    }

    @Override
    public double getDouble(int row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(int row) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(int row) {
        return data[row];
    }

    @Override
    public Object getObject(int row) {
        return data[row];
    }

    public void set(int index, String value) {
        data[index] = value;
    }

    public int size() {
        return size;
    }

    @Override
    public Column filter(BitSet keep) {
        org.bithon.server.datasource.query.pipeline.StringColumn filtered = new org.bithon.server.datasource.query.pipeline.StringColumn(this.name, keep.cardinality());
        for (int i = 0; i < this.size; i++) {
            if (keep.get(i)) {
                filtered.addString(this.data[i]);
            }
        }
        return filtered;
    }

    public String[] getData() {
        return data;
    }

    @Override
    public Column view(int[] selections, int length) {
        return new StringColumnView(this, selections, length);
    }

    private static class StringColumnView extends StringColumn {
        private final StringColumn delegate;
        private final int[] selections;
        private final int length;

        public StringColumnView(StringColumn delegate, int[] selections, int length) {
            super(delegate.getName(), delegate.data, delegate.size);
            this.delegate = delegate;
            this.selections = selections;
            this.length = length;
        }

        @Override
        public String getString(int row) {
            return delegate.getString(selections[row]);
        }

        @Override
        public Object getObject(int row) {
            return delegate.getObject(selections[row]);
        }

        @Override
        public Column filter(BitSet keep) {
            BitSet originalKeep = new BitSet(length);
            for (int i = 0; i < length; i++) {
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

    private void addInternal(String value) {
        if (size >= data.length) {
            String[] newData = new String[(data.length * 3 / 2)];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
        data[size++] = value;
    }
}
