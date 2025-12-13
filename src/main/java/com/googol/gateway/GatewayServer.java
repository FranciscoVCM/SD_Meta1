package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import com.googol.downloaders.Worker;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayServer extends UnicastRemoteObject implements Gateway {

    /* ===============================
          QUEUE + STRUCTURES
       =============================== */

    private final BlockingQueue<String> queue =
            new PriorityBlockingQueue<>(
                    2000,
                    Comparator.comparing(s -> s.startsWith("USER:") ? 0 : 1)
            );

    private final Set<String> seen = ConcurrentHashMap.newKeySet();
    private final List<Worker> workers = new CopyOnWriteArrayList<>();
    private final List<Barrel> barrels;

    private final AtomicInteger pagesIndexed = new AtomicInteger(0);
    private final Map<String, Integer> queryFreq = new ConcurrentHashMap<>();

    private static final int MAX_PAGES = 20000;

    public GatewayServer(List<Barrel> barrels) throws RemoteException {
        super();
        this.barrels = barrels;
    }

    /* ===============================
                URL SUBMISSION
       =============================== */

    @Override
    public synchronized void indexUrl(String url) {
        if (seen.add(url))
            queue.add("USER:" + url);
    }

    private void enqueueUrl(String url) {
        if (url == null || !url.startsWith("http"))
            return;

        if (pagesIndexed.get() >= MAX_PAGES)
            return;

        if (seen.add(url))
            queue.add(url);
    }

    /* ===============================
                DOWNLOADER MGMT
       =============================== */

    @Override
    public synchronized void registerDownloader(Worker w) {
        workers.add(w);
        System.out.println("[Gateway] Worker registered");
    }

    @Override
    public synchronized String getTask() {
        String url = queue.poll();
        if (url == null) return null;
        return url.startsWith("USER:") ? url.substring(5) : url;
    }

    /* ===============================
               RECEIVE CRAWL RESULTS
       =============================== */

    @Override
    public synchronized void submitResult(CrawlResult r) {
        if (r == null || r.url == null)
            return;

        // Fanout para todas as réplicas Barrel
        for (Barrel b : barrels) {
            try {
                b.append(r);
            } catch (Exception ignored) { }
        }

        pagesIndexed.incrementAndGet();

        // submit outlinks to queue
        for (String out : r.outlinks)
            enqueueUrl(out);
    }

    /* ===============================
                  SEARCH
       =============================== */

    @Override
    public synchronized SearchResult search(SearchQuery q) {

        // Update query frequency (top searches)
        queryFreq.merge(q.terms.toLowerCase(), 1, Integer::sum);

        try {
            Barrel b = barrels.get(0);

            // medição correta de latência
            long t0 = System.currentTimeMillis();
            SearchResult res = b.search(q);
            long t1 = System.currentTimeMillis();

            res.lastSearchMs = (t1 - t0);
            return res;

        } catch (RemoteException e) {

            SearchResult empty = new SearchResult();
            empty.items = List.of();
            empty.total = 0;
            empty.lastSearchMs = -1;
            return empty;
        }
    }

    /* ===============================
                LINK ANALYSIS
       =============================== */

    @Override
    public synchronized int inlinks(String url) {
        try {
            return barrels.get(0).inlinks(url);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public synchronized List<String> backlinks(String url) {
        try {
            return barrels.get(0).backlinks(url);
        } catch (Exception e) {
            return List.of();
        }
    }

    /* ===============================
                  STATS
       =============================== */

    @Override
    public synchronized StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();

        // General stats
        s.pagesIndexed = pagesIndexed.get();
        s.urlsInQueue = queue.size();
        s.activeDownloaders = workers.size();

        // Aggregate stats from barrels
        for (Barrel b : barrels) {
            try {
                StatsSnapshot bs = b.barrelStats();
                s.numDocs += bs.numDocs;
                s.numTerms += bs.numTerms;
                s.numPostings += bs.numPostings;

                s.barrelNumDocs.put(b.getName(), bs.numDocs);


                s.barrelAvgLatencySec.put(b.getName(), bs.lastSearchMs / 1000.0);
            } catch (Exception ignored) { }
        }

        // Top queries
        queryFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(e -> s.topQueries.add(
                        e.getKey() + " (" + e.getValue() + ")"
                ));

        return s;
    }
}
