package com.googol.downloaders;

import com.googol.gateway.Gateway;
import com.googol.model.CrawlResult;
import com.googol.util.RmiUtils;

public class DownloaderStandaloneWorker {

    public static void main(String[] args) throws Exception {

        String gwHost = System.getenv().getOrDefault("GATEWAY_HOST", "127.0.0.1");
        int gwPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "1099"));

        Gateway gateway = RmiUtils.lookup(gwHost, gwPort, "Gateway", Gateway.class);

        // Criar stub
        Worker stub = new WorkerImpl();
        RmiUtils.bind("Worker-" + System.nanoTime(), stub);

        // Registar no gateway
        gateway.registerDownloader(stub);
        System.out.println("[Worker] Registered!");

        // Loop — pedir trabalho, fazer crawl, devolver resultado
        while (true) {
            try {
                String url = gateway.getTask();
                if (url == null) {
                    Thread.sleep(200);
                    continue;
                }

                System.out.println("[Worker] Crawling: " + url);
                CrawlResult r = WebCrawler.crawl(url);
                gateway.submitResult(r);

            } catch (Exception e) {
                System.err.println("[Worker] Error: " + e);
                Thread.sleep(500);
            }
        }
    }
}
