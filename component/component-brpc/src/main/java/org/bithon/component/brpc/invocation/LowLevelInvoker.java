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

import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.channel.IBrpcChannel;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;

/**
 * The low-level invoker sends an encoded message on the underlying message channel
 * and receives response from remote on the same channel and then returns the encoded response.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 19:49
 */
public class LowLevelInvoker {

    private final IBrpcChannel channel;
    private final InvocationManager invocationManager;

    public LowLevelInvoker(IBrpcChannel channel, InvocationManager invocationManager) {
        this.channel = channel;
        this.invocationManager = invocationManager;
    }

    public byte[] invoke(ServiceRequestMessageOut serviceRequest, int timeoutMillisecond) throws Throwable {
        return invocationManager.invokeRpc(channel, serviceRequest, timeoutMillisecond);
    }

    /**
     * @param timeoutMillisecond currently not available
     */
    public void invokeStreaming(ServiceRequestMessageOut serviceRequest,
                                StreamResponse<byte[]> streamResponse,
                                int timeoutMillisecond) throws Throwable {
        invocationManager.invokeStreamingRpc(channel, serviceRequest, null, streamResponse, timeoutMillisecond);
    }
}
