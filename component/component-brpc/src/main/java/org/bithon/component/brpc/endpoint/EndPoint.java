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

package org.bithon.component.brpc.endpoint;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

public class EndPoint {
    private final String host;
    private final int port;

    public EndPoint(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static EndPoint of(SocketAddress addr) {
        if (addr instanceof InetSocketAddress) {
            return new EndPoint(((InetSocketAddress) addr).getHostString(), ((InetSocketAddress) addr).getPort());
        }
        return new EndPoint(addr.toString(), 0);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EndPoint endPoint = (EndPoint) o;
        return port == endPoint.port && Objects.equals(host, endPoint.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
