package com.googol.barrels;

import com.googol.util.RmiUtils;

public class BarrelLauncher {
    public static void main(String[] args) throws Exception {
        BarrelReplica impl = new BarrelReplica();
        //RmiUtils.bind("Barrel-" + System.currentTimeMillis(), impl);
        RmiUtils.bind("Barrel1", impl);
        System.out.println("Barrel up as Barrel1");
    }
}