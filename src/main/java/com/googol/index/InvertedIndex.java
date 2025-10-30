package com.googol.index;

import java.util.*;

public class InvertedIndex {
    private final Map<String, Set<String>> map = new HashMap<>(); // termo -> URLs

    public synchronized void add(String term, String url) {
        map.computeIfAbsent(term, k -> new HashSet<>()).add(url);
    }

    public synchronized void addDocument(String url, List<String> terms) {
        for (String t : terms) add(t, url);
    }

    public synchronized Set<String> searchAny(List<String> terms) {
        // união dos conjuntos para termos multi-palavra (modo simples)
        Set<String> acc = new HashSet<>();
        for (String t : terms) {
            Set<String> s = map.get(t);
            if (s != null) acc.addAll(s);
        }
        return acc;
    }
}
