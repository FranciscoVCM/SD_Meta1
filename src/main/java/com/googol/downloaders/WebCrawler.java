package com.googol.downloaders;

import com.googol.model.CrawlResult;

public class WebCrawler implements Runnable {
    private final String url;
    public WebCrawler(String url) { this.url = url; }
    @Override public void run() { /* TODO: fetch + parse + emitir CrawlResult */ }
}