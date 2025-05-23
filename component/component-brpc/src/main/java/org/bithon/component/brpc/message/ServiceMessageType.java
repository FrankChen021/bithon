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

package org.bithon.component.brpc.message;

/**
 * @author frankchen
 */
public class ServiceMessageType {
    public static final int CLIENT_REQUEST = 0x021;

    /**
     * Kept for compatibility
     */
    public static final int CLIENT_REQUEST_ONEWAY = 0x022;

    /**
     * v2 message format with header support
     */
    public static final int CLIENT_REQUEST_V2 = 0x23;

    public static final int SERVER_RESPONSE = 0x515;

    // Streaming message types
    public static final int CLIENT_STREAMING_REQUEST = 0x525;
    public static final int SERVER_STREAMING_DATA = 0x526;
    public static final int SERVER_STREAMING_END = 0x527;
    public static final int CLIENT_STREAMING_CANCEL = 0x528;
}
