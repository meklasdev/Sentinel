package com.wificraft.sentinel.modules.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class IPUtils {
    private static final long[] POWERS_OF_2 = {
        1L << 0, 1L << 1, 1L << 2, 1L << 3, 1L << 4, 1L << 5, 1L << 6, 1L << 7,
        1L << 8, 1L << 9, 1L << 10, 1L << 11, 1L << 12, 1L << 13, 1L << 14, 1L << 15,
        1L << 16, 1L << 17, 1L << 18, 1L << 19, 1L << 20, 1L << 21, 1L << 22, 1L << 23,
        1L << 24, 1L << 25, 1L << 26, 1L << 27, 1L << 28, 1L << 29, 1L << 30, 1L << 31
    };

    public static boolean isIPInRange(String ip, String range) {
        try {
            // Split the range into IP and prefix length
            String[] parts = range.split("/");
            String baseIP = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            // Convert IPs to long
            long ipLong = ipToLong(ip);
            long baseIpLong = ipToLong(baseIP);

            // Calculate mask
            long mask = calculateMask(prefixLength);

            // Apply mask and compare
            return (ipLong & mask) == (baseIpLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private static long ipToLong(String ip) throws UnknownHostException {
        byte[] bytes = InetAddress.getByName(ip).getAddress();
        long result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }

    private static long calculateMask(int prefixLength) {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Prefix length must be between 0 and 32");
        }
        return POWERS_OF_2[32] - 1 - (POWERS_OF_2[32 - prefixLength] - 1);
    }

    public static boolean isPrivateIP(String ip) {
        try {
            long ipLong = ipToLong(ip);
            
            // Check private IP ranges
            return 
                // 10.0.0.0 - 10.255.255.255
                (ipLong >= ipToLong("10.0.0.0") && ipLong <= ipToLong("10.255.255.255")) ||
                // 172.16.0.0 - 172.31.255.255
                (ipLong >= ipToLong("172.16.0.0") && ipLong <= ipToLong("172.31.255.255")) ||
                // 192.168.0.0 - 192.168.255.255
                (ipLong >= ipToLong("192.168.0.0") && ipLong <= ipToLong("192.168.255.255"));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isReservedIP(String ip) {
        try {
            long ipLong = ipToLong(ip);
            
            // Check reserved IP ranges
            return 
                // 0.0.0.0/8
                (ipLong >= ipToLong("0.0.0.0") && ipLong <= ipToLong("0.255.255.255")) ||
                // 127.0.0.0/8
                (ipLong >= ipToLong("127.0.0.0") && ipLong <= ipToLong("127.255.255.255")) ||
                // 169.254.0.0/16
                (ipLong >= ipToLong("169.254.0.0") && ipLong <= ipToLong("169.254.255.255")) ||
                // 192.0.0.0/24
                (ipLong >= ipToLong("192.0.0.0") && ipLong <= ipToLong("192.0.0.255")) ||
                // 192.0.2.0/24
                (ipLong >= ipToLong("192.0.2.0") && ipLong <= ipToLong("192.0.2.255")) ||
                // 192.88.99.0/24
                (ipLong >= ipToLong("192.88.99.0") && ipLong <= ipToLong("192.88.99.255")) ||
                // 198.18.0.0/15
                (ipLong >= ipToLong("198.18.0.0") && ipLong <= ipToLong("198.19.255.255")) ||
                // 198.51.100.0/24
                (ipLong >= ipToLong("198.51.100.0") && ipLong <= ipToLong("198.51.100.255")) ||
                // 203.0.113.0/24
                (ipLong >= ipToLong("203.0.113.0") && ipLong <= ipToLong("203.0.113.255"));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isLoopbackIP(String ip) {
        try {
            return Arrays.equals(InetAddress.getByName(ip).getAddress(), new byte[] {127, 0, 0, 1});
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidIP(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
