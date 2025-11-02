package com.googol.downloaders;

import com.googol.barrels.Barrel;
import com.googol.barrels.ReliableMulticast;
import com.googol.model.CrawlResult;
import com.googol.model.StatsSnapshot;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloaderManager {

    // === Config do crawler===
    private static final int MAX_PAGES = 500;   // orçamento total de páginas
    private static final int MAX_DEPTH = 4;     // profundidade máxima

    // nº de workers/robots configurável
    private volatile int numWorkers = 1;

    // réplicas destino (Barrels)
    private final List<Barrel> barrels;

    // multicast
    private final ReliableMulticast rmcast = new ReliableMulticast();

    //Fila de tarefas (URL + depth)
    private static final class Task {
        final String url;
        final int depth;
        Task(String url, int depth) { this.url = url; this.depth = depth; }
    }
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    private final ThreadPoolExecutor pool =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    private final AtomicInteger rr = new AtomicInteger();          // round-robin para Barrels
    private final AtomicInteger pagesIndexed = new AtomicInteger();
    private volatile boolean started = false;

    public DownloaderManager(List<Barrel> barrels) {
        this.barrels = Objects.requireNonNull(barrels);
        // aplica tamanhos iniciais do pool ao default numWorkers
        pool.setCorePoolSize(numWorkers);
        pool.setMaximumPoolSize(numWorkers);
    }

    /** Define nº de workers. */
    public void setNumWorkers(int n) {
        int v = Math.max(1, n);
        this.numWorkers = v;
        if (!started) {
            pool.setCorePoolSize(v);
            pool.setMaximumPoolSize(v);
        }
    }

    /** Arranca os workers*/
    public synchronized void start() {
        if (started) return;
        started = true;
        for (int i = 0; i < numWorkers; i++) {
            pool.submit(this::worker);
        }
        pool.prestartAllCoreThreads();
    }

    /** Para tudo. */
    public void stop() { pool.shutdownNow(); }

    /** Usado pela Gateway – depth=0. */
    public void submit(String url) { submit(url, 0); }

    /** Usado pelo DownloaderStandalone/Gateway via RMI. */
    public void enqueue(String url, int depth) { submit(url, depth); }

    /** Stats para o ClientApp. */
    public StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();
        s.pagesIndexed      = pagesIndexed.get();
        s.urlsInQueue       = queue.size();
        s.activeDownloaders = pool.getActiveCount();
        return s;
    }

    private Barrel pick() throws Exception {
        if (barrels.isEmpty()) throw new Exception("No barrels available");
        int i = Math.abs(rr.getAndIncrement()) % barrels.size();
        return barrels.get(i);
    }

    private void submit(String url, int depth) {
        if (url == null || url.isBlank()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return;
        if (depth > MAX_DEPTH) return;
        if (pagesIndexed.get() >= MAX_PAGES) return;

        // evita reprocessar a mesma URL
        if (seen.add(url)) {
            queue.offer(new Task(url, depth));
            System.out.println("[Downloader] enqueued (d=" + depth + "): " + url);
            // acorda algum worker à espera
            synchronized (this) { this.notifyAll(); }
        }
    }

    private void worker() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Task t = queue.take();

                if (pagesIndexed.get() >= MAX_PAGES) continue;

                // 1) download + parsing
                CrawlResult r = WebCrawler.crawl(t.url);
                System.out.println("[Downloader] crawled: " + r.url +
                        " (" + r.terms.size() + " terms, " + r.outlinks.size() + " outlinks, depth=" + t.depth + ")");

                // 2) reliable multicast
                boolean storedSomewhere = rmcast.fanout(barrels, r);
                if (storedSomewhere) {
                    pagesIndexed.incrementAndGet();
                } else {
                    System.err.println("[Downloader] fanout: no replica accepted append (skipping count)");
                }

                // 3) submeter outlinks
                int nextDepth = t.depth + 1;
                if (nextDepth <= MAX_DEPTH && pagesIndexed.get() < MAX_PAGES) {
                    if (!r.outlinks.isEmpty()) {
                        System.out.println("[Downloader] submitting " + r.outlinks.size()
                                + " outlinks at depth " + nextDepth + " …");
                    }
                    for (String out : r.outlinks) submit(out, nextDepth);
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[Downloader] worker error: " + e);
            }
        }
    }
}
