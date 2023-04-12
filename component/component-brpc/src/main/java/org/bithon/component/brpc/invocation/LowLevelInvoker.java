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

package org.bithon.component.brpc.invocation;

import org.bithon.component.brpc.channel.IChannelWriter;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 19:49
 */
public class LowLevelInvoker {

    private final IChannelWriter channelWriter;
    private final InvocationManager invocationManager;

    public LowLevelInvoker(IChannelWriter channelWriter, InvocationManager invocationManager) {
        this.channelWriter = channelWriter;
        this.invocationManager = invocationManager;
    }

    public byte[] invoke(ServiceRequestMessageOut serviceRequest, int timeout) throws Throwable {
        return invocationManager.invoke(channelWriter, serviceRequest, timeout);
    }
}
