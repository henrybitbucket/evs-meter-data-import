package com.pa.evs.utils;

import java.io.File;

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

    public static void mkdirs(String dir) {
        try {
            if (new File(dir).isDirectory()) {
                return;
            }
            String[] eles = dir.split("[\\\\/]");
            String tmp = "";
            for (int i = 0; i < eles.length; i++) {
                tmp += eles[i] + '/';
                File f = new File(tmp);
                if (!f.exists()) {
                    f.mkdir();
                }
            }
        } catch (Exception e) {
            //
        }

    }
}



