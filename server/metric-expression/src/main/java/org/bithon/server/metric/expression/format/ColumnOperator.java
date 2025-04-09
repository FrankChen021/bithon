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
                long ret = ((Column.LongColumn) a).getData()[0] + ((Column.LongColumn) b).getData()[0];
                return new Column.LongColumn(name, new long[]{ret});
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long ret = ((Column.LongColumn) a).getData()[0] - ((Column.LongColumn) b).getData()[0];
                return new Column.LongColumn(name, new long[]{ret});
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long ret = ((Column.LongColumn) a).getData()[0] * ((Column.LongColumn) b).getData()[0];
                return new Column.LongColumn(name, new long[]{ret});
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long dividend = ((Column.LongColumn) a).getData()[0];
                long divisor = ((Column.LongColumn) b).getData()[0];
                long ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.LongColumn(name, new long[]{ret});
            };


            //
            // long and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).getData()[0] + ((Column.DoubleColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] + ((Column.LongColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).getData()[0] - ((Column.DoubleColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] - ((Column.LongColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).getData()[0] * ((Column.DoubleColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] * ((Column.LongColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double dividend = ((Column.LongColumn) a).getData()[0];
                double divisor = ((Column.DoubleColumn) b).getData()[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double dividend = ((Column.DoubleColumn) a).getData()[0];
                double divisor = ((Column.LongColumn) b).getData()[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            //
            // double and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] + ((Column.LongColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double ret = ((Column.LongColumn) a).getData()[0] - ((Column.LongColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] * ((Column.LongColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double l = ((Column.DoubleColumn) a).getData()[0];
                long r = ((Column.LongColumn) b).getData()[0];
                double ret = r == 0 ? 0 : l / r;
                return new Column.DoubleColumn(name, new double[]{ret});
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] + ((Column.DoubleColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] - ((Column.DoubleColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double ret = ((Column.DoubleColumn) a).getData()[0] * ((Column.DoubleColumn) b).getData()[0];
                return new Column.DoubleColumn(name, new double[]{ret});
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double dividend = ((Column.DoubleColumn) a).getData()[0];
                double divisor = ((Column.DoubleColumn) b).getData()[0];
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
                long v = ((Column.LongColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.LongColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
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
                long v = ((Column.LongColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long v = ((Column.LongColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            //
            // double and long
            //
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                long[] vector = ((Column.LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double v = ((Column.DoubleColumn) a).getData()[0];
                double[] vector = ((Column.DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
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

    class VectorOverScalarOperator {
        private static final ColumnOperator[][][] OPERATORS = new ColumnOperator[IDataTypeIndex.TYPE_INDEX_SIZE][IDataTypeIndex.TYPE_INDEX_SIZE][4];

        static {
            //
            // long and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.LongColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.LongColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.LongColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
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
                long[] vector = ((Column.LongColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.DoubleColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.DoubleColumn(name, result);

            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.DoubleColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long[] vector = ((Column.LongColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new Column.DoubleColumn(name, result);

            };

            //
            // double and long
            //
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                long v = ((Column.LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new Column.DoubleColumn(name, result);
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new Column.DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new Column.DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new Column.DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double[] vector = ((Column.DoubleColumn) a).getData();
                double v = ((Column.DoubleColumn) b).getData()[0];
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

    class VectorOverVectorOperator {
        private static final ColumnOperator[][][] OPERATORS = new ColumnOperator[IDataTypeIndex.TYPE_INDEX_SIZE][IDataTypeIndex.TYPE_INDEX_SIZE][4];

        static {
            //
            // long and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new Column.LongColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new Column.LongColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new Column.LongColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new Column.LongColumn(name, result);
            };

            //
            // long and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long[] left = ((Column.LongColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            //
            // double and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                long[] right = ((Column.LongColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new Column.DoubleColumn(name, result);
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new Column.DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double[] left = ((Column.DoubleColumn) a).getData();
                double[] right = ((Column.DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size());// Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
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
}
