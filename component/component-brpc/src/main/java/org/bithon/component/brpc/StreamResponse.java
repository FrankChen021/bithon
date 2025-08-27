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
 * <p>
 * Methods that return void and have the last parameter as StreamResponse<T>
 * will be treated as streaming RPC methods.
 * 
 * @param <T> The type of data being streamed
 * @author frankchen
 */
public abstract class StreamResponse<T> {
    private StreamCancellation streamCancellation;

    /**
     * Called when a new data item is received from the stream
     * 
     * @param data The received data item
     */
    public abstract void onNext(T data);
    
    /**
     * Called when an error occurs during streaming
     * 
     * @param throwable The error that occurred
     */
    public abstract void onException(Throwable throwable);
    
    /**
     * Called when the stream is completed successfully
     */
    public abstract void onComplete();
    
    /**
     * Called to check if the stream should be cancelled
     * This method is called periodically during streaming
     * 
     * @return true if the stream should be cancelled, false otherwise
     */
    public boolean isCancelled() {
        StreamCancellation cancellation = getStreamCancellation();
        return cancellation != null && cancellation.isCancelled();
    }
    
    /**
     * Sets a StreamCancellation object that can be used to cancel the stream.
     * This is called by the framework to inject the appropriate cancellation object.
     * 
     * @param cancellation The StreamCancellation object to use
     */
    public final void setStreamCancellation(StreamCancellation cancellation) {
        this.streamCancellation = cancellation;
    }
    
    /**
     * Gets the StreamCancellation object that can be used to cancel the stream.
     * Client code can call getStreamCancellation().cancel() to immediately cancel the stream.
     * 
     * @return The StreamCancellation object, or null if not set
     */
    public final StreamCancellation getStreamCancellation() {
        return this.streamCancellation;
    }

    public void cancel() {
        if (this.streamCancellation != null) {
            this.streamCancellation.cancel();
        }
    }
}
