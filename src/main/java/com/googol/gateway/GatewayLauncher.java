package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.util.RmiUtils;

import java.util.List;

public class GatewayLauncher {
    public static void main(String[] args) throws Exception {
        // IP que esta JVM vai anunciar
        String myIp = System.getenv().getOrDefault("RMI_HOSTNAME", "127.0.0.1");
        System.setProperty("java.rmi.server.hostname", myIp);

        String n1 = (args.length >= 1) ? args[0] : "Barrel1";
        int    p1 = (args.length >= 2) ? Integer.parseInt(args[1]) : 1099;
        String n2 = (args.length >= 3) ? args[2] : "Barrel2";
        int    p2 = (args.length >= 4) ? Integer.parseInt(args[3]) : 1100;

        String h1 = System.getenv().getOrDefault("B1_HOST", "127.0.0.1");
        String h2 = System.getenv().getOrDefault("B2_HOST", "127.0.0.1");

        // Lookups
        Barrel b1 = RmiUtils.lookup(h1, p1, n1, Barrel.class);
        Barrel b2 = RmiUtils.lookup(h2, p2, n2, Barrel.class);

        // Sobe o Gateway e publica no registry (default 1099)
        GatewayServer impl = new GatewayServer(List.of(b1, b2));
        RmiUtils.bind("Gateway", impl, 1099);

        System.out.println("Gateway up @ " + myIp + " (connected to " +
                n1 + "@" + h1 + ":" + p1 + " and " + n2 + "@" + h2 + ":" + p2 + ")");
    }
}
