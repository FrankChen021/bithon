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
 * @date 7/4/25 10:18 am
 */
public interface Column {

    /**
     * Get columnar name
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

    Column filter(BitSet keep);

    Column view(int[] selections, int length);

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
        if (type.equals(IDataType.DATETIME_MILLI.name())) {
            // DATETIME_MILLI is a long
            return new LongColumn(name, initCapacity);
        }
        throw new IllegalArgumentException("Unsupported column type: " + type);
    }

}
