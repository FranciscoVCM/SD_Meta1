package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.util.RmiUtils;

import java.util.List;
import com.googol.downloaders.DownloaderControl;
public class GatewayLauncher {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        // Args esperados (todos opcionais):
        //   <name1> <port1> <name2> <port2>
        // Defaults: Barrel1 1099  Barrel2 1100
        String n1 = (args.length >= 1) ? args[0] : "Barrel1";
        int    p1 = (args.length >= 2) ? Integer.parseInt(args[1]) : 1099;
        String n2 = (args.length >= 3) ? args[2] : "Barrel2";
        int    p2 = (args.length >= 4) ? Integer.parseInt(args[3]) : 1100;

        // localhost; se quiseres hosts diferentes, troca para RmiUtils.lookup("HOST", port, name, Barrel.class)
        Barrel b1 = RmiUtils.lookup("localhost", p1, n1, Barrel.class);
        Barrel b2 = RmiUtils.lookup("localhost", p2, n2, Barrel.class);

        GatewayServer impl = new GatewayServer(List.of(b1, b2));
        RmiUtils.bind("Gateway", impl); // em 1099 por default; muda se precisares

        try {
            var dA = com.googol.util.RmiUtils.lookup(
                    "DownloaderA",
                    com.googol.downloaders.DownloaderControl.class,
                    1099 // mesma registry do Barrel1, é normal partilharem
            );
            impl.registerDownloader(dA);
            System.out.println("Gateway: registado DownloaderA.");
        } catch (Exception e) {
            System.out.println("Gateway: DownloaderA indisponível (ignorado).");
        }

        DownloaderControl dA = RmiUtils.lookup("localhost", 1099, "DownloaderA", DownloaderControl.class); // ajusta host/porta
        impl.registerDownloader(dA);

        System.out.println("Gateway up (connected to " + n1 + ":" + p1 + " and " + n2 + ":" + p2 + ")");
    }
}
