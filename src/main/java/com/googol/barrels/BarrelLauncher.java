package com.googol.barrels;

import com.googol.util.RmiUtils;

public class BarrelLauncher {
    public static void main(String[] args) throws Exception {

        // 1) DEFINIR o hostname ANTES de tudo
        String hostIp = System.getenv().getOrDefault("RMI_HOSTNAME", "127.0.0.1");
        System.setProperty("java.rmi.server.hostname", hostIp);

        // DEBUG
        System.out.println("RMI_HOSTNAME=" + System.getenv("RMI_HOSTNAME"));
        System.out.println("java.rmi.server.hostname=" + System.getProperty("java.rmi.server.hostname"));

        // 2) Ler args: nome + porta + snapshot
        String name = (args.length >= 1) ? args[0] : "Barrel1";
        int    port = (args.length >= 2) ? Integer.parseInt(args[1]) : 1099;
        String snap = (args.length >= 3) ? args[2] : ("barrel-" + name + ".ser");

        // 3) Criar replica
        BarrelReplica impl = new BarrelReplica(name, snap);

        // 4) Publicar RMI
        RmiUtils.bind(name, impl, port);

        System.out.println("Barrel UP → " + name + " @ " + hostIp + ":" + port + " snapshot=" + snap);
    }
}
