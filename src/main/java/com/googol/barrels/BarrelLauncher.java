package com.googol.barrels;

import com.googol.util.RmiUtils;

public class BarrelLauncher {

    public static void main(String[] args) throws Exception {

        String name = args.length >= 1 ? args[0] : "Barrel1";
        int port    = args.length >= 2 ? Integer.parseInt(args[1]) : 1099;
        String file = args.length >= 3 ? args[2] : (name + ".ser");

        String myIp = System.getenv().getOrDefault("RMI_HOSTNAME", "127.0.0.1");
        System.setProperty("java.rmi.server.hostname", myIp);

        BarrelReplica b = new BarrelReplica(name, file);
        RmiUtils.bind(name, b, port);

        System.out.println("[Barrel] Online " + name + "@" + myIp + ":" + port);
    }
}
