package com.googol.downloaders;

import com.googol.gateway.Gateway;
import com.googol.util.RmiUtils;
import com.googol.model.CrawlResult;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class DownloaderStandaloneLauncher {

    /**
     * USO:
     *
     * java DownloaderStandaloneLauncher <numWorkers> seed1 seed2 seed3 ...
     *
     * Exemplo:
     *   java DownloaderStandaloneLauncher 3 https://example.com https://iana.org
     *
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Uso: <numWorkers> <seed1> [seed2] ...");
            return;
        }

        int numWorkers = Integer.parseInt(args[0]);
        List<String> seeds = new ArrayList<>();
        for (int i = 1; i < args.length; i++) seeds.add(args[i]);

        String gwHost = System.getenv().getOrDefault("GATEWAY_HOST", "127.0.0.1");
        int gwPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "1099"));

        System.out.println("Ligando ao Gateway em " + gwHost + ":" + gwPort + " …");

        Gateway gw = RmiUtils.lookup(gwHost, gwPort, "Gateway", Gateway.class);

        /* ================================================
              1 — Criar N workers remotos
           ================================================ */
        for (int i = 0; i < numWorkers; i++) {

            WorkerImpl w = new WorkerImpl();
            String name = "Worker-" + System.nanoTime();

            RmiUtils.bind(name, w);
            gw.registerDownloader(w);

            System.out.println("[Launcher] Worker registado: " + name);

            // iniciar thread de execução
            startWorkerThread(gw, w);
        }

        /* ================================================
              2 — Injetar seeds no Gateway
           ================================================ */
        for (String s : seeds) {
            System.out.println("[Launcher] Seed enviada ao Gateway: " + s);
            gw.indexUrl(s);
        }
    }

    /* =====================================================
            Thread que executa um Worker externo
       ===================================================== */

    private static void startWorkerThread(Gateway gw, WorkerImpl w) {

        new Thread(() -> {
            System.out.println("[WorkerThread] Iniciado…");

            while (true) {
                try {
                    String url = gw.getTask();

                    if (url == null) {
                        Thread.sleep(150);
                        continue;
                    }

                    System.out.println("[WorkerThread] Crawling: " + url);

                    CrawlResult r = WebCrawler.crawl(url);
                    gw.submitResult(r);

                } catch (RemoteException re) {
                    System.err.println("[WorkerThread] RMI falhou: " + re);
                    try { Thread.sleep(1000); } catch (Exception ignored) {}
                } catch (Exception e) {
                    System.err.println("[WorkerThread] Erro: " + e);
                }
            }

        }).start();
    }
}
