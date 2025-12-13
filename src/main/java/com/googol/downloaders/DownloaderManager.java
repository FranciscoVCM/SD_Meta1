package com.googol.downloaders;

import com.googol.barrels.Barrel;
import com.googol.barrels.ReliableMulticast;
import com.googol.model.CrawlResult;
import com.googol.model.StatsSnapshot;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloaderManager {

    /* ==========================================================
                       GLOBAL CRAWLER CONFIG
       ========================================================== */

    private static final int MAX_PAGES = 3000;        // crawling budget
    private static final int MAX_DEPTH = 4;            // recursion depth

    private volatile int numWorkers = 3;               // user-configurable

    private final List<Barrel> barrels;
    private final ReliableMulticast rmcast = new ReliableMulticast();

    private static final class Task {
        final String url;
        final int depth;
        Task(String url, int depth) { this.url = url; this.depth = depth; }
    }

    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    private final ThreadPoolExecutor pool =
            new ThreadPoolExecutor(
                    10,
                    50,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>()
            );

    private final AtomicInteger rr = new AtomicInteger();
    private final AtomicInteger pagesIndexed = new AtomicInteger();

    private volatile boolean started = false;

    /* ==========================================================
                              CONSTRUCTOR
       ========================================================== */

    public DownloaderManager(List<Barrel> barrels) {
        this.barrels = Objects.requireNonNull(barrels);
        pool.setCorePoolSize(numWorkers);
        pool.setMaximumPoolSize(numWorkers);
    }

    /* ==========================================================
                         CONFIGURATION METHODS
       ========================================================== */

    public void setNumWorkers(int n) {
        int v = Math.max(1, n);
        this.numWorkers = v;

        if (!started) {
            pool.setCorePoolSize(v);
            pool.setMaximumPoolSize(v);
        }
    }

    /* ==========================================================
                            LIFECYCLE
       ========================================================== */

    public synchronized void start() {
        if (started) return;
        started = true;

        for (int i = 0; i < numWorkers; i++)
            pool.submit(this::worker);

        pool.prestartAllCoreThreads();
    }

    public void stop() {
        pool.shutdownNow();
    }

    /* ==========================================================
                          GATEWAY ENTRYPOINTS
       ========================================================== */

    public void submit(String url) {
        submit(url, 0);
    }

    public void enqueue(String url, int depth) {
        submit(url, depth);
    }

    /* ==========================================================
                                 STATS
       ========================================================== */

    public StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();
        s.pagesIndexed      = pagesIndexed.get();
        s.urlsInQueue       = queue.size();
        s.activeDownloaders = pool.getActiveCount();
        return s;
    }

    /* ==========================================================
                              INTERNAL LOGIC
       ========================================================== */

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

        if (seen.add(url)) {
            queue.offer(new Task(url, depth));
            System.out.println("[Downloader] enqueued (d=" + depth + "): " + url);

            synchronized (this) { this.notifyAll(); }
        }
    }

    /* ==========================================================
                          WORKER THREAD LOOP
       ========================================================== */

    private void worker() {
        while (!Thread.currentThread().isInterrupted()) {

            try {
                Task t = queue.take();

                if (pagesIndexed.get() >= MAX_PAGES) continue;

                CrawlResult r = WebCrawler.crawl(t.url);

                System.out.println("[Downloader] crawled: " + r.url +
                        " (" + r.terms.size() + " terms, " +
                        r.outlinks.size() + " outlinks, depth=" + t.depth + ")");

                boolean storedSomewhere = rmcast.fanout(barrels, r);

                if (storedSomewhere)
                    pagesIndexed.incrementAndGet();
                else
                    System.err.println("[Downloader] fanout failed — no replica accepted append");

                int nextDepth = t.depth + 1;

                if (nextDepth <= MAX_DEPTH && pagesIndexed.get() < MAX_PAGES) {

                    if (!r.outlinks.isEmpty()) {
                        System.out.println(
                                "[Downloader] submitting " + r.outlinks.size() +
                                        " outlinks at depth " + nextDepth + "…"
                        );
                    }

                    for (String out : r.outlinks)
                        submit(out, nextDepth);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[Downloader] worker error: " + e);
            }
        }
    }
}
