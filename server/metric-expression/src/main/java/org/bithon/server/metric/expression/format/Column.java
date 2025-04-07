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

package org.bithon.server.metric.expression.format;


import org.bithon.component.commons.expression.IDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:18 am
 */
public interface Column<T> {
    T get(int row);

    IDataType getDataType();

    void addObject(Object value);

    void addInt(int value);

    void addLong(long value);

    void addDouble(double value);

    default void addString(String value) {
        addObject(value);
    }

    void set(int index, T value);

    int size();

    void copyFrom(Column<?> source, int index);

    /**
     * Create a new column with the same type and size as this column.
     * NO data will be copied
     */
    Column<T> copy();

    static Column<?> create(String type, int size) {
        if (type.equals(IDataType.STRING.name())) {
            return new StringColumn(size);
        }
        if (type.equals(IDataType.LONG.name())) {
            return new LongColumn(size);
        }
        if (type.equals(IDataType.DOUBLE.name())) {
            return new DoubleColumn(size);
        }

        throw new IllegalArgumentException("Unsupported column type: " + type);
    }

    class LongColumn implements Column<Long> {
        /**
         * declared as package level visibility for performance reason
         */
        final long[] data;
        private int size;

        public LongColumn(int size) {
            this.data = new long[size];
            this.size = 0;
        }

        public LongColumn(long[] data) {
            this.data = data;
            this.size = data.length;
        }

        public Long get(int row) {
            return data[row];
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
        public void set(int index, Long value) {
            data[index] = value;
        }

        public int size() {
            return size;
        }

        @Override
        public void copyFrom(Column<?> source, int index) {
            Object v = source.get(index);
            if (v instanceof Number) {
                addInternal(((Number) v).longValue());
            }
            throw new IllegalArgumentException("Unsupported column type: " + v.getClass().getSimpleName());
        }

        public long[] getData() {
            return data;
        }

        @Override
        public Column<Long> copy() {
            return new LongColumn(size);
        }

        private void addInternal(long value) {
            if (size >= data.length) {
                throw new ArrayIndexOutOfBoundsException("Array is full");
            }
            data[size++] = value;
        }
    }

    class DoubleColumn implements Column<Double> {
        final double[] data;
        int size;

        public DoubleColumn(int size) {
            this.data = new double[size];
        }

        public DoubleColumn(double[] data) {
            this.data = data;
            this.size = data.length;
        }

        public Double get(int row) {
            return data[row];
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

        public void set(int index, Double value) {
            data[index] = value;
        }

        public int size() {
            return size;
        }

        @Override
        public void copyFrom(Column<?> source, int index) {
            Object v = source.get(index);
            if (v instanceof Number) {
                addInternal(((Number) v).doubleValue());
            }
            throw new IllegalArgumentException("Unsupported column type: " + v.getClass().getSimpleName());
        }

        @Override
        public Column<Double> copy() {
            return new DoubleColumn(size);
        }

        public double[] getData() {
            return data;
        }

        private void addInternal(double value) {
            if (size >= data.length) {
                throw new ArrayIndexOutOfBoundsException("Array is full");
            }
            data[size++] = value;
        }
    }

    class StringColumn implements Column<String> {
        private final List<String> data;

        public StringColumn(int size) {
            this.data = new ArrayList<>(size);
        }

        public String get(int row) {
            return data.get(row);
        }

        @Override
        public IDataType getDataType() {
            return IDataType.STRING;
        }

        public void addObject(Object value) {
            if (value instanceof String) {
                data.add((String) value);
            } else {
                data.add(value.toString());
            }
        }

        @Override
        public void addInt(int value) {
            this.data.add(String.valueOf(value));
        }

        @Override
        public void addLong(long value) {
            this.data.add(String.valueOf(value));
        }

        @Override
        public void addDouble(double value) {
            this.data.add(String.valueOf(value));
        }

        public void set(int index, String value) {
            data.set(index, value);
        }

        public int size() {
            return data.size();
        }

        public List<String> getData() {
            return data;
        }

        @Override
        public void copyFrom(Column<?> source, int index) {
            Object v = source.get(index);
            data.add(v.toString());
        }

        @Override
        public Column<String> copy() {
            return new StringColumn(this.data.size());
        }
    }
}
