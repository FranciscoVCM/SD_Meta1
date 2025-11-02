package com.googol.barrels;

import com.googol.util.RmiUtils;

public class BarrelLauncher {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        // args: <name> [port] [snapshotFile]
        String name = (args.length >= 1) ? args[0] : "Barrel1";
        int port    = (args.length >= 2) ? Integer.parseInt(args[1]) : 1099;
        String snap = (args.length >= 3) ? args[2] : ("barrel-" + name + ".ser");

        // para usar entre máquinas definimos o IP público desta máquina
        // System.setProperty("java.rmi.server.hostname", "127.0.0.1");

        BarrelReplica impl = new BarrelReplica(snap);   // <— recebe nome do snapshot
        RmiUtils.bind(name, impl, port);
        System.out.println("Barrel up as " + name + " on port " + port + " (snapshot=" + snap + ")");
    }
}
