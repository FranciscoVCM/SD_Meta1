package com.googol.downloaders;

import com.googol.barrels.Barrel;
import com.googol.gateway.Gateway;
import com.googol.util.RmiUtils;

import java.util.ArrayList;
import java.util.List;

public class DownloaderStandaloneLauncher {
    public static void main(String[] args) throws Exception {
        // 1) hostname do stub deste processo (usa IP da VM quando correres na VM)
        String myIp = System.getenv().getOrDefault("RMI_HOSTNAME",
                (args.length > 0 ? args[0] : "127.0.0.1"));
        System.setProperty("java.rmi.server.hostname", myIp);

        int i = 0;

        // 2) barrels (tripletos host, port, name) até encontrar "--"
        List<Barrel> barrels = new ArrayList<>();
        while (i + 2 < args.length && !"--".equals(args[i])) {
            String host = args[i++];                   // ex.: 192.168.1.81
            int    port = Integer.parseInt(args[i++]); // ex.: 1099
            String name = args[i++];                   // ex.: Barrel1
            Barrel b = RmiUtils.lookup(host, port, name, Barrel.class);
            barrels.add(b);
        }

        if (barrels.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uso: <B_HOST> <B_PORT> <B_NAME> [<B_HOST> <B_PORT> <B_NAME> ...] -- <seed1> [seed2 ...]");
        }

        // 3) separador obrigatório
        if (i < args.length && "--".equals(args[i])) i++;

        // 4) seeds
        List<String> seeds = new ArrayList<>();
        while (i < args.length) seeds.add(args[i++]);

        // 5) manager + controlo
        DownloaderManager dm = new DownloaderManager(barrels);
        dm.start();

        DownloaderControl ctrl = new DownloaderControlImpl(dm);
        RmiUtils.bind("DownloaderA", ctrl, 1099);
        System.out.println("DownloaderStandalone up as DownloaderA (IP=" + myIp + ")");

        // 6) registar-se no Gateway (host/port via env, c/ defaults)
        String gwHost = System.getenv().getOrDefault("GATEWAY_HOST", "192.168.1.81"); // HOST
        int    gwPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT","1099"));

        try {
            for (int t = 0; t < 10; t++) {
                try {
                    Gateway gw = RmiUtils.lookup(gwHost, gwPort, "Gateway", Gateway.class);
                    gw.registerDownloader(ctrl);
                    System.out.println("[Standalone] registado no Gateway @" + gwHost + ":" + gwPort);
                    break;
                } catch (Exception e) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        // 7) submeter seeds locais
        for (String s : seeds) {
            ctrl.enqueue(s, 0);
            System.out.println("[Standalone] submitted seed: " + s);
        }
    }
}

