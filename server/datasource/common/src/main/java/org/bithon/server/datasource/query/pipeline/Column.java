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

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:18 am
 */
public interface Column {

    /**
     * Get columna name
     */
    String getName();

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

    static Column create(String name, IDataType type, int initCapacity) {
        return Column.create(name, type.name(), initCapacity);
    }

    static Column create(String name, String type, int initCapacity) {
        if (type.equals(IDataType.STRING.name())) {
            return new StringColumn(name, initCapacity);
        }
        if (type.equals(IDataType.LONG.name())) {
            return new LongColumn(name, initCapacity);
        }
        if (type.equals(IDataType.DOUBLE.name())) {
            return new DoubleColumn(name, initCapacity);
        }

        throw new IllegalArgumentException("Unsupported column type: " + type);
    }

    class LongColumn implements Column {
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
    }

    class DoubleColumn implements Column {
        private double[] data;
        private int size;
        private final String name;

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

        public double[] getData() {
            return data;
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

    class StringColumn implements Column {
        private String[] data;
        private int size;
        private final String name;

        public StringColumn(String name, int capacity) {
            this.data = new String[capacity];
            this.size = 0;
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

        public String[] getData() {
            return data;
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
}
