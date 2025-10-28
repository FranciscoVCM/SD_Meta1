package com.googol.index;

import java.util.*;

public class InvertedIndex {
    private final Map<String, Set<String>> map = new HashMap<>();
    public void add(String term, String url) { map.computeIfAbsent(term, k -> new HashSet<>()).add(url); }
    public Set<String> get(String term) { return map.getOrDefault(term, Set.of()); }
}