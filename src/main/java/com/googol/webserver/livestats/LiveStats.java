package com.googol.webserver.livestats;

import java.util.List;
import java.util.Map;

public class LiveStats {

    public List<QueryCount> topQueries;
    public Map<String, Double> latency;  // barrelName -> avgLatencySec
    public long lastSearchMs;            // tempo da última pesquisa

    public static class QueryCount {
        public String term;
        public int count;

        public QueryCount(String term, int count) {
            this.term = term;
            this.count = count;
        }
    }
}
