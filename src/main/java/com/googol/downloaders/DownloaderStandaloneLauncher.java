package com.googol.downloaders;

import com.googol.barrels.Barrel;
import com.googol.util.RmiUtils;

import java.util.ArrayList;
import java.util.List;

public class DownloaderStandaloneLauncher {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        int i = 0;

        // === Parte 1: barrels (pares <nome> <porta> até encontrar "--")
        List<Barrel> barrels = new ArrayList<>();
        while (i + 1 < args.length && !"--".equals(args[i])) {
            String name = args[i++];
            int port = Integer.parseInt(args[i++]);   // <- agora o i avança certinho em pares
            Barrel b = RmiUtils.lookup("localhost", port, name, Barrel.class);
            barrels.add(b);
        }

        if (barrels.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uso: <BarrelName1> <Port1> [<BarrelName2> <Port2> ...] -- <seed1> [seed2 ...]");
        }

        // separador obrigatório "--"
        if (i < args.length && "--".equals(args[i])) i++;

        // === Parte 2: seeds (0..n)
        List<String> seeds = new ArrayList<>();
        while (i < args.length) seeds.add(args[i++]);

        // === Manager (+ opcionalmente expor controlo RMI)
        DownloaderManager dm = new DownloaderManager(barrels);
        // se tiveres o método:
        // dm.setNumWorkers(1);
        dm.start();

        // expor controlo (para uma Gateway futura poder fazer enqueue remoto, se quiseres)
        DownloaderControl ctrl = new DownloaderControlImpl(dm);
        RmiUtils.bind("DownloaderA", ctrl);
        System.out.println("DownloaderStandalone up as DownloaderA");

        // submeter seeds locais (arranque)
        for (String s : seeds) {
            ctrl.enqueue(s, 0);
            System.out.println("[Standalone] submitted seed: " + s);
        }
    }
}
