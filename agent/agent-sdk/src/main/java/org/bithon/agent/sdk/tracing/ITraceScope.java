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

package org.bithon.agent.sdk.tracing;

import org.bithon.agent.sdk.tracing.impl.ITraceScopeV1;

/**
 * Represents a tracing scope that can be safely passed between threads
 * and provides context management for cross-thread tracing operations.
 * <p>
 * Use {@link TraceScopeBuilder#attach()} to create an instance of this class.
 * 
 * @author frank.chen021@outlook.com
 * @date 2025/09/07 15:32
 */
public interface ITraceScope extends ITraceScopeV1 {

}
