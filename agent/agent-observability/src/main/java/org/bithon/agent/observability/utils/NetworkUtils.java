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

package org.bithon.agent.observability.utils;

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

    public static NetworkUtils.IpAddress getHostIpAddress() {
        List<InetAddress> localIPs = new ArrayList<>();
        List<InetAddress> netIPs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    boolean has = !ni.isVirtual()
                                  // virtual network interfaces on a host machine that is running virtualization software such as VMware
                                  && !ni.getName().startsWith("vmnet")
                                  // virtual network interfaces on macOS.
                                  && !ni.getName().contains("utun")
                                  // virtual network interfaces on a host machine that is running Oracle VM VirtualBox,
                                  && !ni.getName().contains("vboxnet")
                                  && !ni.getName().startsWith("docker")
                                  && !ni.isLoopback()
                                  && !ni.isPointToPoint()
                                  && ni.isUp();
                    if (has
                        && !ip.isLoopbackAddress()
                        && !ip.getHostAddress().contains(":")) {
                        if (ip.isSiteLocalAddress()) {
                            localIPs.add(ip);
                        } else {
                            netIPs.add(ip);
                        }
                    }
                }
            }
        } catch (SocketException ignored) {
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
