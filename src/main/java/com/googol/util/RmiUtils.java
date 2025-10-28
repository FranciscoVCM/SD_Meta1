package com.googol.util;

import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class RmiUtils {
    private static final int DEFAULT_PORT = 1099;

    /** Garante que existe registry na porta dada; se já existir, reutiliza. */
    private static void ensureRegistry(int port) throws RemoteException {
        try {
            Registry reg = LocateRegistry.getRegistry("localhost", port);
            reg.list(); // ping — lança exceção se não existir
        } catch (Exception e) {
            LocateRegistry.createRegistry(port);
        }
    }

    public static <T extends Remote> void bind(String name, T obj) throws Exception {
        bind(name, obj, DEFAULT_PORT);
    }

    public static <T extends Remote> void bind(String name, T obj, int port) throws Exception {
        ensureRegistry(port);
        Naming.rebind("rmi://localhost:" + port + "/" + name, obj);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Remote> T lookup(String name, Class<T> type) throws Exception {
        return lookup(name, type, DEFAULT_PORT);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Remote> T lookup(String name, Class<T> type, int port) throws Exception {
        return (T) Naming.lookup("rmi://localhost:" + port + "/" + name);
    }
}
