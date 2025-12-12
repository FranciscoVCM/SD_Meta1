package com.googol.downloaders;

import com.googol.gateway.Gateway;
import com.googol.model.CrawlResult;
import com.googol.util.RmiUtils;

public class DownloaderStandaloneWorker {

    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname",
                System.getenv().getOrDefault("RMI_HOSTNAME", "192.168.1.79"));
        String gwHost = System.getenv("GATEWAY_HOST");
        if (gwHost == null || gwHost.isBlank()) {
            gwHost = "192.168.1.79"; // DEFAULT: Gateway
        }
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
