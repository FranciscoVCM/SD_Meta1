package com.googol.index;

import java.util.*;

public class LinkGraph {
    private final Map<String, Set<String>> in = new HashMap<>();
    public void addInlink(String to, String from) { in.computeIfAbsent(to, k->new HashSet<>()).add(from); }
    public int inlinks(String url) { return in.getOrDefault(url, Set.of()).size(); }
}