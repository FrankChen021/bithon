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

package org.bithon.server.sink.metrics.topo;

import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.Measurement;

/**
 * A temp interface to generalize metrics processing of previous http-incoming-metrics/http-outcoming-metrics/...
 * Eventually it will be replaced by some kind of {@link org.bithon.server.storage.datasource.input.TransformSpec}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/8/9 19:30
 */
public interface ITopoTransformer {

    String getSourceType();

    Measurement transform(IInputRow message);
}
