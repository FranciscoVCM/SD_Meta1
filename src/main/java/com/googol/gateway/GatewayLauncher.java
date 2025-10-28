package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.util.RmiUtils;

import java.util.List;

public class GatewayLauncher {
    public static void main(String[] args) throws Exception {
        // liga à réplica que acabámos de lançar
        Barrel b1 = RmiUtils.lookup("Barrel1", Barrel.class);
        GatewayServer impl = new GatewayServer(List.of(b1));
        RmiUtils.bind("Gateway", impl);
        System.out.println("Gateway up (connected to Barrel1)");
    }
}
