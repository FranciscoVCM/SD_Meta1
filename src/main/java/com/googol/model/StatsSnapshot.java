package com.googol.model;

import java.io.Serializable;
import java.util.*;

public class StatsSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    // Contador global de stats
    public int numDocs;
    public int numTerms;
    public int numPostings;

    public long lastSearchMs = 0;

    public List<String> topQueries = new ArrayList<>();

    public Map<String, Integer> barrelNumDocs = new HashMap<>();
    public Map<String, Double> barrelAvgLatencySec = new HashMap<>();

    public int pagesIndexed;
    public int urlsInQueue;
    public int activeDownloaders;
}
