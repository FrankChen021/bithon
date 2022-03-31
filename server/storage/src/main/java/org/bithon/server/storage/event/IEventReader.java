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

package org.bithon.server.storage.event;

import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.metrics.IFilter;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:18 下午
 */
public interface IEventReader extends AutoCloseable {
    List<Event> getEventList(List<IFilter> filters, TimeSpan start, TimeSpan end, int pageNumber, int pageSize);

    int getEventListSize(List<IFilter> filters, TimeSpan start, TimeSpan end);
}
