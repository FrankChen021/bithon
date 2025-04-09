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


import org.bithon.component.commons.expression.IDataTypeIndex;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 6:28 pm
 */
public interface ColumnOperator {

    Column apply(Column a, Column b, String name);

    class ScalarOverScalarOperator {
        private static final ColumnOperator[][][] OPERATORS = new ColumnOperator[IDataTypeIndex.TYPE_INDEX_SIZE][IDataTypeIndex.TYPE_INDEX_SIZE][4];

        static {
            //
            // long and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                long ret = ((Column.LongColumn) a).data[0] + ((Column.LongColumn) b).data[0];
                return new Column.LongColumn(name, new long[]{ret});
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long ret = ((Column.LongColumn) a).data[0] - ((Column.LongColumn) b).data[0];
                return new Column.LongColumn(name, new long[]{ret});
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long ret = ((Column.LongColumn) a).data[0] * ((Column.LongColumn) b).data[0];
                return new Column.LongColumn(name, new long[]{ret});
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long dividend = ((Column.LongColumn) a).data[0];
                long divisor = ((Column.LongColumn) b).data[0];
                long ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.LongColumn(name, new long[]{ret});
            };


            //
            // long and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).data[0] + ((Column.DoubleColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).data[0] + ((Column.LongColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).data[0] - ((Column.DoubleColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).data[0] - ((Column.LongColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).data[0] * ((Column.DoubleColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).data[0] * ((Column.LongColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double dividend = ((Column.LongColumn) a).data[0];
                double divisor = ((Column.DoubleColumn) b).data[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double dividend = ((Column.DoubleColumn) a).data[0];
                double divisor = ((Column.LongColumn) b).data[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).data[0] + ((Column.DoubleColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).data[0] - ((Column.DoubleColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).data[0] * ((Column.DoubleColumn) b).data[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double dividend = ((Column.DoubleColumn) a).data[0];
                double divisor = ((Column.DoubleColumn) b).data[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.DoubleColumn(name, new double[]{ret});
            };
        }

        public static Column apply(Column left, Column right, String name, int operator) {
            ColumnOperator columnOperator = OPERATORS[left.getDataType().getTypeIndex()][right.getDataType().getTypeIndex()][operator];
            if (columnOperator == null) {
                throw new IllegalStateException(StringUtils.format("Unsupported operation %s on type %s and %s", operator, left.getDataType(), right.getDataType()));
            }
            return columnOperator.apply(left, right, name);
        }
    }

    class ScalarOverVectorOperator {
        private static final ColumnOperator[][][] OPERATORS = new ColumnOperator[IDataTypeIndex.TYPE_INDEX_SIZE][IDataTypeIndex.TYPE_INDEX_SIZE][4];

        static {
            //
            // long and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            //
            // long and double
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                double[] vector = ((Column.DoubleColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                double[] vector = ((Column.DoubleColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                double[] vector = ((Column.DoubleColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).data[0];
                double[] vector = ((Column.DoubleColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).data[0];
                long[] vector = ((Column.LongColumn) b).data;
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // TODO: double and double
        }

        public static Column apply(Column left, Column right, String name, int operator) {
            ColumnOperator columnOperator = OPERATORS[left.getDataType().getTypeIndex()][right.getDataType().getTypeIndex()][operator];
            if (columnOperator == null) {
                throw new IllegalStateException(StringUtils.format("Unsupported operation %s on type %s and %s", operator, left.getDataType(), right.getDataType()));
            }
            return columnOperator.apply(left, right, name);
        }
    }

    class VectorOverScalarOperator {
        private static final ColumnOperator[][][] OPERATORS = new ColumnOperator[IDataTypeIndex.TYPE_INDEX_SIZE][IDataTypeIndex.TYPE_INDEX_SIZE][4];

        static {
            //
            // long and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.LongColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.LongColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.LongColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new Column.LongColumn(name, result);
            };

            // long and double
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                double v = ((Column.DoubleColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.DoubleColumn(name, result);

            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.DoubleColumn(name, result);

            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                double v = ((Column.DoubleColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.DoubleColumn(name, result);

            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.DoubleColumn(name, result);

            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                double v = ((Column.DoubleColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.DoubleColumn(name, result);

            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.DoubleColumn(name, result);

            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).data;
                double v = ((Column.DoubleColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new Column.DoubleColumn(name, result);

            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).data;
                long v = ((Column.LongColumn) b).data[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new Column.DoubleColumn(name, result);

            };
        }

        public static Column apply(Column left, Column right, String name, int operator) {
            ColumnOperator columnOperator = OPERATORS[left.getDataType().getTypeIndex()][right.getDataType().getTypeIndex()][operator];
            if (columnOperator == null) {
                throw new IllegalStateException(StringUtils.format("Unsupported operation %s on type %s and %s", operator, left.getDataType(), right.getDataType()));
            }
            return columnOperator.apply(left, right, name);
        }
    }

//
//    public static Column plus(Column a, Column b) {
//        if (a.getDataType() == IDataType.DOUBLE || b.getDataType() == IDataType.DOUBLE) {
//            return plusAsDoubles(a, b);
//        } else {
//            return plusAsLongs(a, b);
//        }
//    }
//
//    public static Column.LongColumn plusAsLongs(Column a, Column b) {
//        int size = a.size();
//        long[] result = new long[size];
//        for (int i = 0; i < size; i++) {
//            result[i] = a.getAsLong(i) + b.getAsLong(i);
//        }
//        return result;
//    }
//
//    public static Column.DoubleColumn plusAsDoubles(Column a, Column b) {
//        int n = Math.min(a.size(), b.size());
//        Double[] result = new Double[n];
//        for (int i = 0; i < n; i++) {
//            result[i] = a.getAsDouble(i) + b.getAsDouble(i);
//        }
//        return result;
//    }
}
