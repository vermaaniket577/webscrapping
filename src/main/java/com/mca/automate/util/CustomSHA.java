package com.mca.automate.util;

import org.springframework.stereotype.Component;

/* JADX INFO: loaded from: CustomSHA.class */
@Component
public class CustomSHA {
    public String calcSHA(String str) {
        int[] x = str2blks_SHA1(str);
        int[] w = new int[80];
        int a = 1732584193;
        int b = -271733879;
        int c = -1732584194;
        int d = 271733878;
        int e = -1009589776;
        for (int i = 0; i < x.length; i += 16) {
            int olda = a;
            int oldb = b;
            int oldc = c;
            int oldd = d;
            int olde = e;
            for (int j = 0; j < 80; j++) {
                if (j < 16) {
                    w[j] = x[i + j];
                } else {
                    w[j] = rol(((w[j - 3] ^ w[j - 8]) ^ w[j - 14]) ^ w[j - 16], 1);
                }
                int t = add(add(rol(a, 5), ft(j, b, c, d)), add(add(e, w[j]), kt(j)));
                e = d;
                d = c;
                c = rol(b, 30);
                b = a;
                a = t;
            }
            a = add(a, olda);
            b = add(b, oldb);
            c = add(c, oldc);
            d = add(d, oldd);
            e = add(e, olde);
        }
        return hex(a) + hex(b) + hex(c) + hex(d) + hex(e);
    }

    public static int[] str2blks_SHA1(String str) {
        int nblk = ((str.length() + 8) >> 6) + 1;
        int[] blks = new int[nblk * 16];
        for (int i = 0; i < nblk * 16; i++) {
            blks[i] = 0;
        }
        int i2 = 0;
        while (i2 < str.length()) {
            int i3 = i2 >> 2;
            blks[i3] = blks[i3] | ((str.charAt(i2) & 255) << (24 - ((i2 % 4) * 8)));
            i2++;
        }
        int i4 = i2 >> 2;
        blks[i4] = blks[i4] | (128 << (24 - ((i2 % 4) * 8)));
        blks[(nblk * 16) - 1] = str.length() * 8;
        return blks;
    }

    private static int rol(int value, int bits) {
        return (value << bits) | (value >>> (32 - bits));
    }

    private static int ft(int t, int b, int c, int d) {
        return t < 20 ? (b & c) | ((b ^ (-1)) & d) : t < 40 ? (b ^ c) ^ d : t < 60 ? (b & c) | (b & d) | (c & d) : (b ^ c) ^ d;
    }

    private static int kt(int t) {
        if (t < 20) {
            return 1518500249;
        }
        if (t < 40) {
            return 1859775393;
        }
        return t < 60 ? -1894007588 : -899497514;
    }

    private static int add(int x, int y) {
        long z = ((long) x) + ((long) y);
        return (int) (z & 4294967295L);
    }

    private static String hex(int val) {
        String hexString = Integer.toHexString(val);
        while (true) {
            String s = hexString;
            if (s.length() >= 8) {
                return s;
            }
            hexString = "0" + s;
        }
    }
}
