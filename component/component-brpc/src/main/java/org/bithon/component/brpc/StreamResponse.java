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
 * Interface for handling streaming responses in RPC calls.
 * 
 * Methods that return void and have the last parameter as StreamResponse<T>
 * will be treated as streaming RPC methods.
 * 
 * @param <T> The type of data being streamed
 * @author frankchen
 */
public interface StreamResponse<T> {
    
    /**
     * Called when a new data item is received from the stream
     * 
     * @param data The received data item
     */
    void onNext(T data);
    
    /**
     * Called when an error occurs during streaming
     * 
     * @param throwable The error that occurred
     */
    void onException(Throwable throwable);
    
    /**
     * Called when the stream is completed successfully
     */
    void onComplete();
    
    /**
     * Called to check if the stream should be cancelled
     * This method is called periodically during streaming
     * 
     * @return true if the stream should be cancelled, false otherwise
     */
    default boolean isCancelled() {
        return false;
    }
} 
