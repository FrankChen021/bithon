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

package com.sbss.bithon.agent.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author frankchen
 */
public class NetworkUtils {

    public static NetworkUtils.IpAddress getIpAddress() {
        List<InetAddress> localIPs = new ArrayList<>();
        List<InetAddress> netIPs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    boolean has = !ni.isLoopback() && !ni.isPointToPoint() && !ni.isVirtual();
                    has = has && !ni.getName().contains("vmnet") && !ni.getName().contains("utun") &&
                          !ni.getName().contains("vboxnet");
                    if (ni.isUp() && has && !ip.isLoopbackAddress() && !ip.getHostAddress().contains(":")) {
                        if (ip.isSiteLocalAddress()) {
                            localIPs.add(ip);
                        } else {
                            netIPs.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            // Ignore
        }
        return new NetworkUtils.IpAddress(localIPs, netIPs);
    }

    public static class IpAddress {

        final List<InetAddress> localIPs;
        final List<InetAddress> netIPs;

        IpAddress(List<InetAddress> localIPs, List<InetAddress> netIPs) {
            this.localIPs = localIPs;
            this.netIPs = netIPs;
        }

        public InetAddress getInetAddress() {
            return null != netIPs &&
                   !netIPs.isEmpty() ? netIPs.get(0)
                                     : (null != localIPs && !localIPs.isEmpty() ? localIPs.get(0)
                                                                                : getDefaultInetAddress());
        }

        public InetAddress getLocalInetAddress() {
            return null != localIPs &&
                   !localIPs.isEmpty() ? localIPs.get(0)
                                       : (null != netIPs && !netIPs.isEmpty() ? netIPs.get(0)
                                                                              : getDefaultInetAddress());
        }

        private InetAddress getDefaultInetAddress() {
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
                // Ignore
            }
            return inetAddress;
        }
    }
}
