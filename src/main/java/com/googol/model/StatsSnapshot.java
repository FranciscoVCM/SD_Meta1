package com.googol.model;

import java.io.Serializable;
import java.util.*;

public class StatsSnapshot implements Serializable {
    public int pagesIndexed;
    public int urlsInQueue;
    public int activeDownloaders;

    public int numDocs;
    public int numTerms;
    public int numPostings;

    public long lastSearchMs;

    public List<String> topQueries = new ArrayList<>();

    public Map<String, Integer> barrelNumDocs = new LinkedHashMap<>();
    public Map<String, Double> barrelAvgLatencySec = new LinkedHashMap<>();
}
