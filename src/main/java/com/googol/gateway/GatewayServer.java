package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.downloaders.DownloaderManager;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayServer extends UnicastRemoteObject implements Gateway {

    private final List<Barrel> barrels;
    private final AtomicInteger rr = new AtomicInteger();
    private final DownloaderManager downloader;

    public GatewayServer(List<Barrel> barrels) throws RemoteException {
        super();
        this.barrels = barrels;
        this.downloader = new DownloaderManager(barrels);
        this.downloader.start(); // arranca workers
    }

    private Barrel pick() { return barrels.get(Math.abs(rr.getAndIncrement() % barrels.size())); }

    @Override
    public void indexUrl(String url) throws RemoteException {
        downloader.submit(url);
        System.out.println("Enqueued: " + url);
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        // escolher um barrel (por agora, o primeiro registado)
        if (barrels.isEmpty()) throw new RemoteException("No barrels available");
        Barrel b = barrels.get(0);
        try {
            return b.search(q);
        } catch (RemoteException e) {
            // tentativa de failover simples
            for (int i = 1; i < barrels.size(); i++) {
                try { return barrels.get(i).search(q); }
                catch (RemoteException ignore) {}
            }
            throw e;
        }
    }

    @Override
    public int inlinks(String url) throws RemoteException {
        if (barrels.isEmpty()) return 0;
        return pick().inlinks(url);
    }

    @Override
    public String stats() throws RemoteException {
        return "Gateway OK";
    }
}
