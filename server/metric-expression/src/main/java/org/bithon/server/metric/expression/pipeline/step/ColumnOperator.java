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

package org.bithon.server.metric.expression.pipeline.step;


import org.bithon.component.commons.expression.IDataTypeIndex;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.DoubleColumn;
import org.bithon.server.datasource.query.pipeline.LongColumn;

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
                long ret = ((LongColumn) a).getData()[0] + ((LongColumn) b).getData()[0];
                return new LongColumn(name, new long[]{ret});
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long ret = ((LongColumn) a).getData()[0] - ((LongColumn) b).getData()[0];
                return new LongColumn(name, new long[]{ret});
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long ret = ((LongColumn) a).getData()[0] * ((LongColumn) b).getData()[0];
                return new LongColumn(name, new long[]{ret});
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long dividend = ((LongColumn) a).getData()[0];
                long divisor = ((LongColumn) b).getData()[0];
                long ret = divisor == 0 ? 0 : dividend / divisor;
                return new LongColumn(name, new long[]{ret});
            };

            //
            // long and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double ret = ((LongColumn) a).getData()[0] + ((DoubleColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] + ((LongColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double ret = ((LongColumn) a).getData()[0] - ((DoubleColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] - ((LongColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double ret = ((LongColumn) a).getData()[0] * ((DoubleColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] * ((LongColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double dividend = ((LongColumn) a).getData()[0];
                double divisor = ((DoubleColumn) b).getData()[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double dividend = ((DoubleColumn) a).getData()[0];
                double divisor = ((LongColumn) b).getData()[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new DoubleColumn(name, new double[]{ret});
            };

            //
            // double and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] + ((LongColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double ret = ((LongColumn) a).getData()[0] - ((LongColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] * ((LongColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double l = ((DoubleColumn) a).getData()[0];
                long r = ((LongColumn) b).getData()[0];
                double ret = r == 0 ? 0 : l / r;
                return new DoubleColumn(name, new double[]{ret});
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] + ((DoubleColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] - ((DoubleColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double ret = ((DoubleColumn) a).getData()[0] * ((DoubleColumn) b).getData()[0];
                return new DoubleColumn(name, new double[]{ret});
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double dividend = ((DoubleColumn) a).getData()[0];
                double divisor = ((DoubleColumn) b).getData()[0];
                double ret = divisor == 0 ? 0 : dividend / divisor;
                return new DoubleColumn(name, new double[]{ret});
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
                long v = ((LongColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new LongColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new LongColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new LongColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                long[] result = new long[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new LongColumn(name, result);
            };

            //
            // long and double
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new DoubleColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new DoubleColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new DoubleColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long v = ((LongColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new DoubleColumn(name, result);
            };

            //
            // double and long
            //
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                long[] vector = ((LongColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new DoubleColumn(name, result);
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v + vector[i];
                }
                return new DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v - vector[i];
                }
                return new DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = v * vector[i];
                }
                return new DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double v = ((DoubleColumn) a).getData()[0];
                double[] vector = ((DoubleColumn) b).getData();
                int size = b.size();
                double[] result = new double[size];
                for (int i = 0; i < b.size(); i++) {
                    result[i] = vector[i] == 0 ? 0 : v / vector[i];
                }
                return new DoubleColumn(name, result);
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
                long[] vector = ((LongColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new LongColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new LongColumn(name, result);
            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new LongColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new LongColumn(name, result);
            };

            // long and double
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new DoubleColumn(name, result);
            };

            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new DoubleColumn(name, result);

            };

            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new DoubleColumn(name, result);
            };

            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long[] vector = ((LongColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new DoubleColumn(name, result);

            };

            //
            // double and long
            //
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new DoubleColumn(name, result);
            };
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                long v = ((LongColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new DoubleColumn(name, result);
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] + v;
                }
                return new DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] - v;
                }
                return new DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = vector[i] * v;
                }
                return new DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double[] vector = ((DoubleColumn) a).getData();
                double v = ((DoubleColumn) b).getData()[0];
                int size = a.size();
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = v == 0 ? 0 : vector[i] / v;
                }
                return new DoubleColumn(name, result);
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
                long[] left = ((LongColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new LongColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new LongColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new LongColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                long[] result = new long[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new LongColumn(name, result);
            };

            //
            // long and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_LONG][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                long[] left = ((LongColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new DoubleColumn(name, result);
            };

            //
            // double and long
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][0] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][1] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][2] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_LONG][3] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                long[] right = ((LongColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new DoubleColumn(name, result);
            };

            //
            // double and double
            //
            // Plus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][0] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] + right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Minus
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][1] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] - right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Multiply
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][2] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = left[i] * right[i];
                }
                return new DoubleColumn(name, result);
            };
            // Divide
            OPERATORS[IDataTypeIndex.TYPE_INDEX_DOUBLE][IDataTypeIndex.TYPE_INDEX_DOUBLE][3] = (a, b, name) -> {
                double[] left = ((DoubleColumn) a).getData();
                double[] right = ((DoubleColumn) b).getData();
                int size = Math.min(a.size(), b.size()); // Use min to make it safe
                double[] result = new double[size];
                for (int i = 0; i < size; i++) {
                    result[i] = right[i] == 0 ? 0 : left[i] / right[i];
                }
                return new DoubleColumn(name, result);
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
