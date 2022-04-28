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

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.DateTimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author frankchen
 * @date 2020-08-26 15:51:55
 */
public class BaselineCalibrator {

    private final Map<Integer, List<String>> baseTime = new ConcurrentHashMap<>();

    public List<String> getBaseTimePoint(int windowLengthInMinute) {
        return baseTime.computeIfAbsent(windowLengthInMinute, windowLength -> {
            int size = 24 * 60 / windowLength;
            List<String> timePoints = new ArrayList<>(size);
            long time = DateTimeUtils.getDayStart(System.currentTimeMillis());
            for (int i = 0; i < size; i++) {
                timePoints.add(StringUtils.format("HH:mm", time));
                time += windowLength * 60 * 1000L;
            }
            return timePoints;
        });
    }

    public List<Map<String, Object>> calibrate(List<Map<String, Object>> baseline,
                                               int windowLength,
                                               String metricName) {
        List<Map<String, Object>> calibrated;

        //
        // 补齐基准数据
        //
        List<String> timePoints = getBaseTimePoint(windowLength);
        if (baseline.size() != timePoints.size()) {
            // 使用TreeMap保持数据有序
            Map<String, Map<String, Object>> data = baseline.stream()
                                                            .collect(Collectors.toMap(key -> (String) key.get("__time"),
                                                                                      val -> val,
                                                                                      (u,
                                                                                       v) -> {
                                                                                          throw new IllegalStateException(StringUtils.format("Duplicate key %s",
                                                                                                                                             u));
                                                                                      },
                                                                                      TreeMap::new));
            timePoints.forEach((time) -> data.computeIfAbsent(time, key -> new HashMap<>()));

            calibrated = new ArrayList<>(data.size());
            calibrated.addAll(data.values());
        } else {
            calibrated = baseline;
        }

        //
        // 平滑处理
        //
        ISmoothAlgorithm smoothAlogrithm = SmoothAlgorithmFactory.create(SmoothAlgorithm.SMA);
        if (smoothAlogrithm != null) {
            //noinspection unchecked
            smoothAlogrithm.smooth(baseline,
                                   val -> (Number) ((Map<String, Object>) val).get(metricName),
                                   (o,
                                    v) -> ((Map<String, Object>) o).put(metricName, v));
        }

        return calibrated;
    }
}
