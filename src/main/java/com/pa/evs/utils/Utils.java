package com.pa.evs.utils;

public class Utils {
    public static String formatMac(String mac) {
        char[] c = mac.toUpperCase().replace(":", "").toCharArray();
        mac = "";
        for (int i = 0; i < c.length; i++) {
            if (i % 2 == 0 && i > 0 && (i != c.length - 1)) {
                mac += ":";
            }
            mac += c[i];
        }

        return mac;
    }
}



