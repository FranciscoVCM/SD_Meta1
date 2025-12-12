package com.googol.barrels;

import com.googol.util.RmiUtils;

public class BarrelLauncher {

    public static void main(String[] args) throws Exception {

        String name = args.length >= 1 ? args[0] : "Barrel1";
        int port = args.length >= 2 ? Integer.parseInt(args[1]) : 2001;

        String snapshot = "barrel-" + name + ".ser";

        BarrelReplica impl = new BarrelReplica(name, snapshot);

        RmiUtils.bind(name, impl, port);

        System.out.println("[BarrelLauncher] " + name + " running on port " + port);
        System.out.println("[BarrelLauncher] Snapshot file = " + snapshot);
    }
}
