package com.googol.model;

import java.io.Serializable;
import java.util.*;

public class StatsSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    // Totais globais
    public int numDocs = 0;
    public int numTerms = 0;
    public int numPostings = 0;
    public int pagesIndexed = 0;
    public int urlsInQueue = 0;
    public int activeDownloaders = 0;

    // Latência por barrel (Ms convertido para segundos)
    public Map<String, Double> barrelAvgLatencySec = new HashMap<>();

    // Nº de documentos por barrel
    public Map<String, Integer> barrelNumDocs = new HashMap<>();

    // Top 10 queries
    public List<String> topQueries = new ArrayList<>();
}
