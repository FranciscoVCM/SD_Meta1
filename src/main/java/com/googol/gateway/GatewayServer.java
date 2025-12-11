package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.downloaders.DownloaderManager;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class GatewayServer extends UnicastRemoteObject implements Gateway {

    private final List<Barrel> barrels;
    private volatile long lastSearchMs = -1;
    private final AtomicInteger rr = new AtomicInteger();
    private final DownloaderManager downloader;
    private final List<com.googol.downloaders.DownloaderControl> downloaders = new ArrayList<>();

    // === EX6: métricas
    private final ConcurrentHashMap<String, Integer> queryFreq = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LongAdder> barrelOkCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LongAdder> barrelLatencyMs = new ConcurrentHashMap<>();

    private String barrelLabel(int idx) {
        return "Barrel" + (idx + 1);
    }
    public void registerDownloader(com.googol.downloaders.DownloaderControl dc) {
        if (dc != null) this.downloaders.add(dc);
    }

    public GatewayServer(List<Barrel> barrels) throws RemoteException {
        super();
        this.barrels = barrels;
        this.downloader = new DownloaderManager(barrels); // workers + fila estão no manager
        this.downloader.start();
    }

    // round-robin simples
    private Barrel pick() throws RemoteException {
        if (barrels == null || barrels.isEmpty()) {
            throw new RemoteException("No barrels available");
        }
        int i = Math.abs(rr.getAndIncrement()) % barrels.size();
        return barrels.get(i);
    }

    @Override
    public void indexUrl(String url) throws RemoteException {
        downloader.submit(url);              // enfileira; os workers vão seguir outlinks também
        System.out.println("Enqueued: " + url);
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        if (barrels.isEmpty()) throw new RemoteException("No barrels available");

        // === EX6: contar query
        String key = (q.terms == null) ? "" : q.terms.toLowerCase().trim();
        if (!key.isBlank()) {
            queryFreq.merge(key, 1, Integer::sum);
        }

        // tenta em round robin e se falhar, vai tentando os restantes
        int start = Math.abs(rr.getAndIncrement()) % barrels.size();
        for (int k = 0; k < barrels.size(); k++) {
            int idx = (start + k) % barrels.size();
            Barrel b = barrels.get(idx);
            long t0 = System.nanoTime();
            try {
                SearchResult r = b.search(q);

                //registar latência do barrel
                long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
                barrelOkCount.computeIfAbsent(idx, i -> new LongAdder()).increment();
                barrelLatencyMs.computeIfAbsent(idx, i -> new LongAdder()).add(elapsedMs);
                this.lastSearchMs = elapsedMs;
                return r;
            } catch (RemoteException e) {
                // tenta próximo
            }
        }
        throw new RemoteException("All barrels unavailable");
    }

    @Override
    public int inlinks(String url) throws RemoteException {
        return pick().inlinks(url);
    }

    @Override
    public synchronized List<String> backlinks(String url) throws RemoteException {
        if (barrels.isEmpty()) throw new RemoteException("No barrels available");
        int start = Math.abs(rr.get()) % barrels.size();
        for (int k = 0; k < barrels.size(); k++) {
            int idx = (start + k) % barrels.size();
            try {
                return barrels.get(idx).backlinks(url);
            } catch (RemoteException e) {
                // tenta próximo
            }
        }
        throw new RemoteException("All barrels unavailable");
    }
    @Override
    public StatsSnapshot stats() throws RemoteException {
        StatsSnapshot out = new StatsSnapshot();

        // === (1) Métricas dos Barrels (numDocs/numTerms/numPostings)
        int totalDocs = 0, totalTerms = 0, totalPostings = 0;
        for (int i = 0; i < barrels.size(); i++) {
            try {
                StatsSnapshot s = barrels.get(i).stats();
                totalDocs     += s.numDocs;
                totalTerms    += s.numTerms;
                totalPostings += s.numPostings;

                out.barrelNumDocs.put(barrelLabel(i), s.numDocs);
            } catch (RemoteException e) {
                out.barrelNumDocs.put(barrelLabel(i), -1); // -1 = indisponível
            }
        }
        out.numDocs     = totalDocs;
        out.numTerms    = totalTerms;
        out.numPostings = totalPostings;

        // === (2) Métricas de Downloaders
        // 2a) Local
        int pagesIndexedSum = 0, urlsInQueueSum = 0, activeDlSum = 0;
        if (this.downloader != null) {
            try {
                StatsSnapshot d = this.downloader.stats();
                pagesIndexedSum += d.pagesIndexed;
                urlsInQueueSum  += d.urlsInQueue;
                activeDlSum     += d.activeDownloaders;
            } catch (Exception ignore) { /* best-effort */ }
        }

        // 2b) Remotos (DownloaderStandalone via RMI)
        for (com.googol.downloaders.DownloaderControl dc : this.downloaders) {
            if (dc == null) continue;
            try {
                com.googol.model.StatsSnapshot d = dc.downloaderStats();
                if (d != null) {
                    pagesIndexedSum += d.pagesIndexed;
                    urlsInQueueSum  += d.urlsInQueue;
                    activeDlSum     += d.activeDownloaders;
                }
            } catch (Exception ignore) {
                // downloader remoto offline — ignora
            }
        }
        out.pagesIndexed      = pagesIndexedSum;
        out.urlsInQueue       = urlsInQueueSum;
        out.activeDownloaders = activeDlSum;

        // === (3) Top-10 queries
        PriorityQueue<Map.Entry<String,Integer>> pq =
                new PriorityQueue<>((a,b) -> {
                    int c = Integer.compare(b.getValue(), a.getValue());
                    return (c != 0) ? c : a.getKey().compareTo(b.getKey());
                });
        pq.addAll(queryFreq.entrySet());
        int limit = 10;
        while (!pq.isEmpty() && limit-- > 0) {
            Map.Entry<String,Integer> e = pq.poll();
            out.topQueries.add(e.getKey() + " (" + e.getValue() + ")");
        }

        // === (4) Latência média por Barrel em segundos
        for (int i = 0; i < barrels.size(); i++) {
            long cnt   = Optional.ofNullable(barrelOkCount.get(i)).map(LongAdder::sum).orElse(0L);
            long sumMs = Optional.ofNullable(barrelLatencyMs.get(i)).map(LongAdder::sum).orElse(0L);
            double avgSec = (cnt > 0) ? ((sumMs * 1.0 / cnt) / 1000.0) : -1.0; // ms -> s
            out.barrelAvgLatencySec.put(barrelLabel(i), avgSec);
        }
        out.lastSearchMs = this.lastSearchMs;
        return out;
    }

}
