package com.sbss.bithon.agent.core.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtil {

    public static NetworkUtil.IpAddress getIpAddress() {
        List<InetAddress> localIPs = new ArrayList<>();
        List<InetAddress> netIPs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = addresses.nextElement();
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
        return new NetworkUtil.IpAddress(localIPs, netIPs);
    }

    public static class IpAddress {

        List<InetAddress> localIPs, netIPs;

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
