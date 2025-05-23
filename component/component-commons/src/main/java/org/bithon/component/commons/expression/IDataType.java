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

package org.bithon.component.commons.expression;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.bithon.component.commons.expression.IDataTypeIndex.TYPE_INDEX_STRING;

/**
 * @author Frank Chen
 * @date 30/6/23 5:22 pm
 */
public enum IDataType {
    STRING {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            return STRING.equals(dataType);
        }

        @Override
        public String format(Number value) {
            return null;
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            return false;
        }

        @Override
        public Number diff(Number left, Number right) {
            return null;
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            return value;
        }

        @Override
        public int getTypeIndex() {
            return TYPE_INDEX_STRING;
        }
    },

    LONG {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            return dataType == LONG || dataType == DOUBLE;
        }

        @Override
        public String format(Number value) {
            return new DecimalFormat("#,###",
                                     DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(value.longValue());
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            return left.longValue() > right.longValue();
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            return left.longValue() >= right.longValue();
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            return left.longValue() < right.longValue();
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            return left.longValue() <= right.longValue();
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            return left.longValue() == right.longValue();
        }

        @Override
        public Number diff(Number left, Number right) {
            return left.longValue() - right.longValue();
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            return value;
        }

        @Override
        public int getTypeIndex() {
            return IDataTypeIndex.TYPE_INDEX_LONG;
        }
    },

    BOOLEAN {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            return dataType == BOOLEAN;
        }

        @Override
        public String format(Number value) {
            return new DecimalFormat("#,###.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(value.doubleValue());
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            return left.doubleValue() > right.doubleValue();
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            return left.doubleValue() >= right.doubleValue();
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            return left.doubleValue() < right.doubleValue();
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            return left.doubleValue() <= right.doubleValue();
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            return left.doubleValue() == right.doubleValue();
        }

        @Override
        public Number diff(Number left, Number right) {
            return left.doubleValue() - right.doubleValue();
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            return BigDecimal.valueOf(value.doubleValue()).setScale(scale, RoundingMode.HALF_UP).doubleValue();
        }

        @Override
        public int getTypeIndex() {
            return IDataTypeIndex.TYPE_INDEX_BOOLEAN;
        }
    },

    DOUBLE {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            return dataType == LONG || dataType == DOUBLE;
        }

        @Override
        public String format(Number value) {
            return new DecimalFormat("#,###.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(value.doubleValue());
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            return left.doubleValue() > right.doubleValue();
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            return left.doubleValue() >= right.doubleValue();
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            return left.doubleValue() < right.doubleValue();
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            return left.doubleValue() <= right.doubleValue();
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            return left.doubleValue() == right.doubleValue();
        }

        @Override
        public Number diff(Number left, Number right) {
            return left.doubleValue() - right.doubleValue();
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            return BigDecimal.valueOf(value.doubleValue()).setScale(scale, RoundingMode.HALF_UP).doubleValue();
        }

        @Override
        public int getTypeIndex() {
            return IDataTypeIndex.TYPE_INDEX_DOUBLE;
        }
    },

    /**
     * The DateTime that stores the time in milliseconds
     */
    DATETIME_MILLI {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            return false;
        }

        @Override
        public String format(Number value) {
            return null;
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            return left.doubleValue() > right.doubleValue();
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            return false;
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            return false;
        }

        @Override
        public Number diff(Number left, Number right) {
            return null;
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            return null;
        }

        @Override
        public int getTypeIndex() {
            return IDataTypeIndex.TYPE_INDEX_DATETIME_MILLI;
        }
    },

    OBJECT {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String format(Number value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Number diff(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getTypeIndex() {
            return IDataTypeIndex.TYPE_INDEX_OBJECT;
        }
    },

    ARRAY {
        @Override
        public boolean canCastFrom(IDataType dataType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String format(Number value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGreaterThan(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isGreaterThanOrEqual(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLessThan(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isLessThanOrEqual(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEqual(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Number diff(Number left, Number right) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Number scaleTo(Number value, int scale) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getTypeIndex() {
            return IDataTypeIndex.TYPE_INDEX_ARRAY;
        }
    };

    public abstract boolean canCastFrom(IDataType dataType);

    public abstract String format(Number value);

    public abstract boolean isGreaterThan(Number left, Number right);

    public abstract boolean isGreaterThanOrEqual(Number left, Number right);

    public abstract boolean isLessThan(Number left, Number right);

    public abstract boolean isLessThanOrEqual(Number left, Number right);

    public abstract boolean isEqual(Number left, Number right);

    public abstract Number diff(Number left, Number right);

    public abstract Number scaleTo(Number value, int scale);

    public abstract int getTypeIndex();
}
