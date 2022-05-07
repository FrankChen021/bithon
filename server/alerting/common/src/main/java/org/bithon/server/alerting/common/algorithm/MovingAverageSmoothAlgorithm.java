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

package org.bithon.server.alerting.common.algorithm;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Simple Moving Average
 *
 * @author frankchen
 * @date 2020-03-27 09:08:20
 */
public class MovingAverageSmoothAlgorithm implements ISmoothAlgorithm {

    @Override
    public List<Number> smooth(List<Number> data) {
        SMAImpl impl = new SMAImpl(5);
        data.replaceAll(number -> impl.addNewNumber(number.doubleValue()));
        return data;
    }

    @Override
    public void smooth(List<?> data,
                       Function<Object, ? extends Number> getter,
                       BiConsumer<Object, Double> setter) {
        SMAImpl impl = new SMAImpl(5);
        for (Object obj : data) {
            Number n = getter.apply(obj);
            setter.accept(obj, impl.addNewNumber(n.doubleValue()));
        }
    }

    public static class SMAImpl {
        private final int period;
        private final double[] window;
        private double sum = 0.0;
        private int pointer = 0;
        private int size = 0;

        public SMAImpl(int windowSize) {
            if (windowSize < 1) {
                throw new IllegalArgumentException("period must be > 0");
            }
            this.period = windowSize;
            window = new double[windowSize];
        }

        public double addNewNumber(double number) {
            sum += number;
            if (size < period) {
                window[pointer++] = number;
                size++;
            } else {
                pointer = pointer % period;
                sum -= window[pointer];
                window[pointer++] = number;
            }
            return sum / size;
        }
    }
}
