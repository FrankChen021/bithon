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

package org.bithon.component.brpc;

import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.endpoint.EndPoint;

/**
 * A controller on {@link BrpcService}
 *
 *
 * @author frankchen
 */
public interface IServiceController {
    void debug(boolean on);

    /**
     * @param timeout milli-second
     */
    void setTimeout(long timeout);

    void rstTimeout();

    EndPoint getPeer();

    IBrpcChannel getChannel();
}
