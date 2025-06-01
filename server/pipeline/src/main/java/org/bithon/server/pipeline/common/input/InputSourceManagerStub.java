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

package org.bithon.server.pipeline.common.input;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Frank Chen
 * @date 31/3/24 7:20 pm
 */
@Slf4j
public class InputSourceManagerStub implements IInputSourceManager {
    @Override
    public void start(Class<? extends IInputSource> inputSourceClazz) {
        log.warn("Starting of input source {} failed due to metric store is not enabled", inputSourceClazz.getSimpleName());
    }
}
