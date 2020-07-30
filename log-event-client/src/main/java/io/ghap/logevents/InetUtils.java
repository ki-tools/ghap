package io.ghap.logevents;

import java.net.*;
import java.util.Enumeration;

public class InetUtils {
    private InetUtils(){}

    /*
    public static void main(String[] args) throws SocketException {
        String ip = getLocalIP(false);
        System.out.println("---> " + ip);
    }
    */

    public static String getLocalIP(boolean withLocal) throws SocketException {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            // continue with another logic
        }

        Enumeration<NetworkInterface> iterNetwork;
        Enumeration<InetAddress> iterAddress;
        NetworkInterface network;
        InetAddress address;

        iterNetwork = NetworkInterface.getNetworkInterfaces();

        while (iterNetwork.hasMoreElements()) {
            network = iterNetwork.nextElement();

            if (!network.isUp())
                continue;

            if (network.isLoopback())  // If I want loopback, I would use "localhost" or "127.0.0.1".
                continue;

            iterAddress = network.getInetAddresses();

            while (iterAddress.hasMoreElements()){
                address = iterAddress.nextElement();

                if (address.isLoopbackAddress())
                    continue;

                if (address.isMulticastAddress())
                    continue;

                if (address.isAnyLocalAddress() && !withLocal)
                    continue;

                if (!(address instanceof Inet4Address)){
                    continue;
                }

                return(address.getHostAddress());
            }
        }

        return withLocal ? null:getLocalIP(true);
    }

}
