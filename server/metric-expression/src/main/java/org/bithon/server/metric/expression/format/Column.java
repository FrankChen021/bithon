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

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:18 am
 */
public interface Column {
    IDataType getDataType();

    void addObject(Object value);

    void addInt(int value);

    void addLong(long value);

    void addDouble(double value);

    default void addString(String value) {
        addObject(value);
    }

    double getDouble(int row);

    int getInt(int row);

    long getLong(int row);

    default String getString(int row) {
        throw new UnsupportedOperationException();
    }

    Object getObject(int row);

    int size();

    static Column create(String type, int size) {
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

    class LongColumn implements Column {
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

        public long[] getData() {
            return data;
        }

        private void addInternal(long value) {
            if (size >= data.length) {
                throw new ArrayIndexOutOfBoundsException("Array is full");
            }
            data[size++] = value;
        }
    }

    class DoubleColumn implements Column {
        final double[] data;
        int size;

        public DoubleColumn(int size) {
            this.data = new double[size];
        }

        public DoubleColumn(double[] data) {
            this.data = data;
            this.size = data.length;
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

    class StringColumn implements Column {
        final String[] data;
        private int size;

        public StringColumn(int capacity) {
            this.data = new String[capacity];
            this.size = 0;
        }

        public String get(int row) {
            return data[row];
        }

        @Override
        public IDataType getDataType() {
            return IDataType.STRING;
        }

        public void addObject(Object value) {
            if (value instanceof String) {
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

        public String[] getData() {
            return data;
        }

        private void addInternal(String value) {
            if (size >= data.length) {
                throw new ArrayIndexOutOfBoundsException("Array is full");
            }
            data[size++] = value;
        }
    }
}
