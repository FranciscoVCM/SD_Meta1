package com.googol.downloaders;

import java.util.concurrent.*;

public class DownloaderManager implements WebCrawlerInterface {
    private final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final java.util.Set<String> inFlight = ConcurrentHashMap.newKeySet();

    @Override public void submit(String url) {
        if (inFlight.add(url)) pool.submit(() -> { try { new WebCrawler(url).run(); } finally { inFlight.remove(url); }});
    }
}