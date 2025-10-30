package com.googol.downloaders;

import com.googol.barrels.Barrel;
import com.googol.model.CrawlResult;

import java.util.List;
import java.util.concurrent.*;

public class DownloaderManager implements WebCrawlerInterface, AutoCloseable {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final ExecutorService pool;
    private final List<Barrel> barrels;
    private volatile boolean started = false;

    public DownloaderManager(List<Barrel> barrels) {
        this.barrels = barrels;
        int n = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.pool = Executors.newFixedThreadPool(n);
    }

    public synchronized void start() {
        if (started) return;
        started = true;
        // workers que consomem URLs e chamam o crawler
        for (int i = 0; i < Math.max(2, Runtime.getRuntime().availableProcessors()/2); i++) {
            pool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String url = queue.take();
                        CrawlResult r = WebCrawler.crawl(url);
                        // envia para o primeiro barrel (por agora)
                        if (!barrels.isEmpty() && r != null) {
                            try { barrels.get(0).append(r); } catch (Exception ignored) {}
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

    @Override
    public void submit(String url) {
        if (url == null || url.isBlank()) return;
        queue.offer(url);
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }
}
