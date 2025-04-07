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


import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:18 am
 */
public interface Column<T> {
    T get(int row);
    void add(T value);
    int size();
    List<T> getData(); // useful for result export


    class IntColumn implements Column<Integer> {
        private final List<Integer> data;

        public IntColumn(int size) {
            this.data = new ArrayList<>(size);
        }

        public Integer get(int row) {
            return data.get(row);
        }

        public void add(Integer value) {
            data.add(value);
        }

        public int size() {
            return data.size();
        }

        public List<Integer> getData() {
            return data;
        }
    }

    class StringColumn implements Column<String> {
        private final List<String> data = new ArrayList<>();

        public String get(int row) {
            return data.get(row);
        }

        public void add(String value) {
            data.add(value);
        }

        public int size() {
            return data.size();
        }

        public List<String> getData() {
            return data;
        }
    }
}
