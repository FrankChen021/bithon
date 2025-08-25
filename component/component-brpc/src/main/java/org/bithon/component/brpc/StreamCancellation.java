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

/**
 * Interface for controlling streaming RPC cancellation.
 * This object is returned by streaming RPC methods to allow clients to cancel the stream.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/8/25 20:00
 */
public interface StreamCancellation {
    
    /**
     * Cancel the streaming RPC immediately.
     * This will send a cancellation message to the server and stop the stream.
     */
    void cancel();
    
    /**
     * Check if the stream has been cancelled.
     * 
     * @return true if the stream has been cancelled, false otherwise
     */
    boolean isCancelled();
}
