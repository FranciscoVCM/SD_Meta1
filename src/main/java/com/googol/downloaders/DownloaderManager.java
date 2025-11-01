package com.googol.downloaders;

import com.googol.barrels.Barrel;
import com.googol.model.CrawlResult;
import com.googol.model.StatsSnapshot;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloaderManager {

    // === Config do crawler (ajusta à vontade)
    private static final int NUM_WORKERS = 2;
    private static final int MAX_PAGES   = 1000;  // orçamento total de páginas
    private static final int MAX_DEPTH   = 5;    // 0 = só a seed

    private final List<Barrel> barrels;

    // Tarefa = URL + profundidade
    private static final class Task {
        final String url;
        final int depth;
        Task(String url, int depth) { this.url = url; this.depth = depth; }
    }

    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final Set<String> seen = ConcurrentHashMap.newKeySet();

    private final ThreadPoolExecutor pool;        // precisamos disto p/ activeCount
    private final AtomicInteger rr = new AtomicInteger();
    private final AtomicInteger pagesIndexed = new AtomicInteger();

    public DownloaderManager(List<Barrel> barrels) {
        this.barrels = barrels;
        this.pool = new ThreadPoolExecutor(
                NUM_WORKERS, NUM_WORKERS, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()
        );
        this.pool.prestartAllCoreThreads();
    }

    public void start() {
        for (int i = 0; i < NUM_WORKERS; i++) pool.submit(this::worker);
    }

    public void stop() { pool.shutdownNow(); }

    private Barrel pick() throws RemoteException {
        if (barrels == null || barrels.isEmpty())
            throw new RemoteException("No barrels available");
        int i = Math.abs(rr.getAndIncrement()) % barrels.size();
        return barrels.get(i);
    }

    // API chamada pelo Gateway
    public void submit(String url) { submit(url, 0); }

    private void submit(String url, int depth) {
        if (url == null || url.isBlank()) return;
        if (!url.startsWith("http://") && !url.startsWith("https://")) return;
        if (depth > MAX_DEPTH) return;
        if (pagesIndexed.get() >= MAX_PAGES) return;

        if (seen.add(url)) {
            queue.offer(new Task(url, depth));
            System.out.println("[Downloader] enqueued (d=" + depth + "): " + url);
        }
    }

    public StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();
        s.pagesIndexed      = pagesIndexed.get();
        s.urlsInQueue       = queue.size();
        s.activeDownloaders = pool.getActiveCount();
        return s;
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

                // 2) tenta indexar
                try {
                    pick().append(r);
                    pagesIndexed.incrementAndGet();
                } catch (RemoteException e) {
                    System.err.println("[Downloader] append failed: " + e);
                }

                // 3) tentar submeter outlinks (mesmo que o append falhe)
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

