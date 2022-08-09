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

package org.bithon.server.sink.metrics.transformer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/9 19:40
 */
public class TopoTransformers {

    private static final TopoTransformers INSTANCE = new TopoTransformers();

    public static TopoTransformers getInstance() {
        return INSTANCE;
    }

    private final Map<String, ITopoTransformer> topoTransformers = new ConcurrentHashMap<>();

    public TopoTransformers() {
        this.addTopoTransformer(new HttpIncomingMetricTopoTransformer());
    }

    public void addTopoTransformer(ITopoTransformer transformer) {
        if (transformer != null) {
            this.topoTransformers.put(transformer.getSourceType(), transformer);
        }
    }

    public ITopoTransformer getTopoTransformer(String metricType) {
        return topoTransformers.get(metricType);
    }
}
