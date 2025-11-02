package com.googol.barrels;

import com.googol.util.RmiUtils;

public class BarrelLauncher {
    public static void main(String[] args) throws Exception {
        // IP que esta JVM vai anunciar nos stubs RMI
        String hostIp = System.getenv().getOrDefault("RMI_HOSTNAME", "127.0.0.1");
        System.setProperty("java.rmi.server.hostname", hostIp);

        // args: <name> [port] [snapshotFile]
        String name = (args.length >= 1) ? args[0] : "Barrel1";
        int    port = (args.length >= 2) ? Integer.parseInt(args[1]) : 1099;
        String snap = (args.length >= 3) ? args[2] : ("barrel-" + name + ".ser");

        BarrelReplica impl = new BarrelReplica(snap);
        RmiUtils.bind(name, impl, port);

        System.out.println("Barrel up as " + name + " on port " + port +
                " (snapshot=" + snap + ", hostname=" + hostIp + ")");
    }
}
