/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.webapp.services;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/8 8:13 下午
 */
@Service
public class ServiceDiscovery {
    private String apiHost;

    public ServiceDiscovery(Environment env) {
        apiHost = System.getProperty("bithon.api.host");
        if (!StringUtils.isEmpty(apiHost)) {
            return;
        }

        // check if webapp is running together with collector for standalone mode
        boolean isApiHost = Arrays.asList(env.getActiveProfiles()).contains("collector");
        if (!isApiHost) {
            //TODO: use service discovery
            throw new IllegalStateException("-Dbithon.api.host not specified for web-app");
        }

        InetAddress addr = findFirstNonLoopbackAddress();
        if (addr == null) {
            throw new IllegalStateException("-Dbithon.api.host not specified for web-app");
        }

        apiHost = String.format("http://%s:%s", addr.getHostAddress(), env.getProperty("server.port"));
    }

    public String getApiHost() {
        return apiHost;
    }

    /**
     * taken from {@link org.springframework.cloud.commons.util.InetUtils#findFirstNonLoopbackAddress()}
     */
    private InetAddress findFirstNonLoopbackAddress() {
        InetAddress result = null;
        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface
                .getNetworkInterfaces(); nics.hasMoreElements(); ) {
                NetworkInterface ifc = nics.nextElement();
                if (!ifc.isUp()) {
                    continue;
                }

                if (ifc.getIndex() < lowest || result == null) {
                    lowest = ifc.getIndex();
                } else if (result != null) {
                    continue;
                }

                for (Enumeration<InetAddress> addrs = ifc
                    .getInetAddresses(); addrs.hasMoreElements(); ) {
                    InetAddress address = addrs.nextElement();
                    if (address instanceof Inet4Address
                        && !address.isLoopbackAddress()) {
                        result = address;
                    }
                }
            }

        } catch (IOException ignored) {
        }

        if (result != null) {
            return result;
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException ignored) {
        }

        return null;
    }
}
