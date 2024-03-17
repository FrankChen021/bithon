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

package org.bithon.component.commons.utils;

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

    public static InetAddress getIpAddress() {
        List<InetAddress> localIPs = new ArrayList<>();
        List<InetAddress> netIPs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaceList = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaceList.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaceList.nextElement();
                boolean valid = !networkInterface.isVirtual()
                                // virtual network interfaces on a host machine that is running virtualization software such as VMware
                                && !networkInterface.getName().startsWith("vmnet")
                                // virtual network interfaces on macOS.
                                && !networkInterface.getName().contains("utun")
                                // virtual network interfaces on a host machine that is running Oracle VM VirtualBox,
                                && !networkInterface.getName().contains("vboxnet")
                                && !networkInterface.getName().startsWith("docker")
                                && !networkInterface.isLoopback()
                                && !networkInterface.isPointToPoint()
                                && networkInterface.isUp();

                if (!valid) {
                    continue;
                }

                Enumeration<InetAddress> addressList = networkInterface.getInetAddresses();
                while (addressList.hasMoreElements()) {
                    InetAddress ip = addressList.nextElement();
                    if (!ip.isLoopbackAddress()
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

        return new NetworkUtils.IpAddress(localIPs, netIPs).getAddress();
    }

    static class IpAddress {

        final List<InetAddress> localIPs;
        final List<InetAddress> netIPs;

        private IpAddress(List<InetAddress> localIPs, List<InetAddress> netIPs) {
            this.localIPs = localIPs;
            this.netIPs = netIPs;
        }

        public InetAddress getAddress() {
            InetAddress addr = getInetAddress();
            return addr != null ? addr : getLocalInetAddress();
        }

        public InetAddress getInetAddress() {
            return !netIPs.isEmpty() ? netIPs.get(0) : (!localIPs.isEmpty() ? localIPs.get(0) : getDefaultInetAddress());
        }

        public InetAddress getLocalInetAddress() {
            return !localIPs.isEmpty() ? localIPs.get(0) : (!netIPs.isEmpty() ? netIPs.get(0) : getDefaultInetAddress());
        }

        private InetAddress getDefaultInetAddress() {
            InetAddress inetAddress = null;
            try {
                inetAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException ignored) {
            }
            return inetAddress;
        }
    }
}
