package com.googol.gateway;

import java.util.Comparator;

public class UrlPriorityComparator implements Comparator<String> {

    @Override
    public int compare(String a, String b) {
        boolean aUser = a.startsWith("USER:");
        boolean bUser = b.startsWith("USER:");

        if (aUser && !bUser) return -1;   // USER primeiro
        if (bUser && !aUser) return 1;    // USER primeiro

        return 0; // mesma prioridade
    }
}
