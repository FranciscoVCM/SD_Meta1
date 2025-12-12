package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import com.googol.downloaders.WebCrawler;
import com.googol.downloaders.Worker;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayServer extends UnicastRemoteObject implements Gateway {

    // ================================
    //  FILA GLOBAL
    // ================================
    private final BlockingQueue<String> queue =
            new PriorityBlockingQueue<>(1000, new UrlPriorityComparator());
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    // Workers remotos
    private final List<Worker> workers = new CopyOnWriteArrayList<>();

    // Barrels conectados
    private final List<Barrel> barrels;

    // Limites
    private static final int MAX_PAGES = 5000;
    private final AtomicInteger pagesIndexed = new AtomicInteger(0);

    public GatewayServer(List<Barrel> barrels) throws RemoteException {
        super();
        this.barrels = barrels;
    }

    // =============================
    //          API principal
    // =============================
    @Override
    public synchronized void indexUrl(String url) throws RemoteException {
        queue.add("USER:" + url);
        seen.add(url); // manter controle
    }

    private void submitUrl(String url) {
        if (url == null || url.isBlank()) return;
        if (!url.startsWith("http")) return;
        if (pagesIndexed.get() >= MAX_PAGES) return;

        if (seen.add(url)) {
            queue.offer(url);
            System.out.println("[Gateway] URL enqueued: " + url);
        }
    }

    // =============================
    //        API PARA WORKERS
    // =============================
    @Override
    public synchronized void registerDownloader(Worker w) {
        if (w != null) {
            workers.add(w);
            System.out.println("[Gateway] Worker registered");
        }
    }

    @Override
    public synchronized String getTask() {
        String url = queue.poll();
        if (url == null) return null;

        if (url.startsWith("USER:"))
            return url.substring(5);

        return url;
    }

    @Override
    public synchronized void submitResult(CrawlResult r) throws RemoteException {
        if (r == null || r.url == null) return;

        try {
            // enviar para todos os barrels
            for (Barrel b : barrels) {
                try {
                    b.append(r);
                } catch (Exception e) {
                    System.err.println("[Gateway] Barrel unreachable: " + b + " cause=" + e);
                }
            }

            pagesIndexed.incrementAndGet();

            // enfileirar novos links
            for (String out : r.outlinks) {
                queue.add(out);
            }

        } catch (Exception e) {
            System.err.println("[Gateway] Failed to process crawl result: " + e);
        }
    }

    // --- Internal worker (opcional)
    public void startInternalWorkers(int n) {
        for (int i = 0; i < n; i++) {
            new Thread(() -> {
                while (true) {
                    try {
                        String url = getTask();
                        if (url == null) {
                            Thread.sleep(150);
                            continue;
                        }
                        CrawlResult r = WebCrawler.crawl(url);
                        submitResult(r);
                    } catch (Exception ignored) {}
                }
            }).start();
        }
    }

    // =============================
    //      Funcionalidades antigas
    // =============================

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        return barrels.get(0).search(q);
    }

    @Override
    public synchronized int inlinks(String url) throws RemoteException {
        return barrels.get(0).inlinks(url);
    }

    @Override
    public synchronized List<String> backlinks(String url) throws RemoteException {
        return barrels.get(0).backlinks(url);
    }

    @Override
    public synchronized StatsSnapshot stats() throws RemoteException {

        StatsSnapshot s = new StatsSnapshot();
        s.pagesIndexed = pagesIndexed.get();
        s.urlsInQueue = queue.size();
        s.activeDownloaders = workers.size();

        int totalDocs = 0;
        int totalTerms = 0;
        int totalPostings = 0;

        for (Barrel b : barrels) {
            try {
                StatsSnapshot bs = b.barrelStats();

                totalDocs += bs.numDocs;
                totalTerms += bs.numTerms;
                totalPostings += bs.numPostings;

                s.barrelNumDocs.put(b.toString(), bs.numDocs);
                s.barrelAvgLatencySec.put(b.toString(), bs.lastSearchMs / 1000.0);

            } catch (Exception ignored) {}
        }

        s.numDocs = totalDocs;
        s.numTerms = totalTerms;
        s.numPostings = totalPostings;

        return s;
    }
}
