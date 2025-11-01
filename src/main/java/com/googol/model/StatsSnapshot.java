package com.googol.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatsSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public int pagesIndexed;
    public int urlsInQueue;
    public int activeDownloaders;
    public int numDocs;
    public int numTerms;
    public int numPostings;

    // top-10 termos/queries mais usados (ordenados por frequência desc)
    public List<String> topQueries = new ArrayList<>();

    // nº de documentos por Barrel (mantém a ordem de inserção p/ leitura humana)
    // labelDoBarrel -> numDocs
    public Map<String, Integer> barrelNumDocs = new LinkedHashMap<>();

    // latência média por Barrel em décimos de segundo
    public Map<String, Double> barrelAvgLatencySec = new LinkedHashMap<>();
}
