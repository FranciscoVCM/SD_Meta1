package com.googol.util;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RmiUtils {
    private static final int DEFAULT_PORT = 1099;

    /** Garante que existe um registry na porta dada (localhost). */
    private static void ensureRegistry(int port) {
        try {
            Registry reg = LocateRegistry.getRegistry("localhost", port);
            reg.list(); // “ping”: lança se não existir
        } catch (Exception e) {
            try {
                LocateRegistry.createRegistry(port);
            } catch (Exception ignore) { /* já deve existir */ }
        }
    }

    // ===== bind

    public static <T extends Remote> void bind(String name, T obj) throws Exception {
        bind(name, obj, DEFAULT_PORT);
    }

    public static <T extends Remote> void bind(String name, T obj, int port) throws Exception {
        ensureRegistry(port);
        Naming.rebind("rmi://localhost:" + port + "/" + name, obj);
    }

    // ===== lookup (localhost)

    public static <T extends Remote> T lookup(String name, Class<T> type) throws Exception {
        return lookup("localhost", DEFAULT_PORT, name, type);
    }

    public static <T extends Remote> T lookup(String name, Class<T> type, int port) throws Exception {
        return lookup("localhost", port, name, type);
    }

    // ===== lookup (host + port)

    public static <T extends Remote> T lookup(String host, int port, String name, Class<T> type) throws Exception {
        Object obj = Naming.lookup("rmi://" + host + ":" + port + "/" + name);
        return type.cast(obj);
    }
}

