package com.googol.gateway;

import java.util.Comparator;

public class UrlPriorityComparator implements Comparator<String> {

    @Override
    public int compare(String a, String b) {

        int sa = a.length();
        int sb = b.length();

        if (sa != sb) return Integer.compare(sa, sb);

        return a.compareTo(b);
    }
}

