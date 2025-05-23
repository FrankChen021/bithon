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

package org.bithon.component.brpc.message.in;

import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;

import java.io.IOException;

/**
 * Message for receiving streaming cancel requests on the server side
 * 
 * @author frankchen
 */
public class ServiceStreamingCancelMessageIn extends ServiceMessageIn {
    
    @Override
    public int getMessageType() {
        return ServiceMessageType.CLIENT_STREAMING_CANCEL;
    }
    
    @Override
    public ServiceMessage decode(CodedInputStream in) throws IOException {
        this.transactionId = in.readInt64();
        return this;
    }
}
