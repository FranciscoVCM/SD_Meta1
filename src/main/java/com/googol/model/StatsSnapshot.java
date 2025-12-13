package com.googol.model;

import java.io.Serializable;
import java.util.*;

/**
 * Estatísticas agregadas do sistema.
 * Este objeto é construído no Gateway e enviado ao WebServer.
 *
 * Totalmente compatível com:
 *  - BarrelReplica (latência, numDocs, numTerms, numPostings)
 *  - GatewayServer (pagesIndexed, queue, workers, topQueries)
 *  - UI /stats
 */
public class StatsSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==============================
    // GATEWAY GLOBAL
    // ==============================

    /** Nº total de páginas indexadas por todos os workers */
    public int pagesIndexed = 0;

    /** Nº de URLs ainda na queue do gateway */
    public int urlsInQueue = 0;

    /** Número de downloaders ativos no sistema */
    public int activeDownloaders = 0;

    // ==============================
    // BARRELS (agregados globais)
    // ==============================

    /** Nº total de documentos (somando todos os barrels) */
    public int numDocs = 0;

    /** Nº total de termos únicos no índice */
    public int numTerms = 0;

    /** Nº total de postings (pares termo->documento) */
    public int numPostings = 0;

    // ==============================
    // DETALHES POR BARREL
    // ==============================

    /**
     * Nº de documentos por barrel:
     *    map.get("Barrel-1") = 1532
     */
    public Map<String, Integer> barrelNumDocs = new LinkedHashMap<>();

    /**
     * Latência média do último search (em segundos):
     *    map.get("Barrel-1") = 0.083
     */
    public Map<String, Double> barrelAvgLatencySec = new LinkedHashMap<>();

    // ==============================
    // TOP QUERIES
    // ==============================

    /**
     * Lista ordenada das top queries:
     * Ex: ["java (12)", "pokemon (9)", "clash royale (5)"]
     */
    public List<String> topQueries = new ArrayList<>();

    // ==============================
    // Métodos utilitários opcionais
    // ==============================

    public void addBarrelStats(String barrelName, int docs, double latencySec) {
        barrelNumDocs.put(barrelName, docs);
        barrelAvgLatencySec.put(barrelName, latencySec);
    }

    public void addTopQuery(String s) {
        if (s != null) topQueries.add(s);
    }
}

