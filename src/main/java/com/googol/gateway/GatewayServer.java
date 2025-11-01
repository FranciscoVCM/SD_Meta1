package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.downloaders.DownloaderManager;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

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
        this.downloader = new DownloaderManager(barrels); // workers + fila estão no manager
        this.downloader.start();
    }

    // round-robin robusto
    private Barrel pick() throws RemoteException {
        if (barrels == null || barrels.isEmpty()) {
            throw new RemoteException("No barrels available");
        }
        int i = Math.abs(rr.getAndIncrement()) % barrels.size();
        return barrels.get(i);
    }

    @Override
    public void indexUrl(String url) throws RemoteException {
        downloader.submit(url);              // enfileira; os workers vão seguir outlinks também
        System.out.println("Enqueued: " + url);
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        if (barrels.isEmpty()) throw new RemoteException("No barrels available");

        // tenta o barrel em round-robin; se falhar, tenta os restantes (failover simples)
        int start = Math.abs(rr.get()) % barrels.size();
        for (int k = 0; k < barrels.size(); k++) {
            int idx = (start + k) % barrels.size();
            try {
                return barrels.get(idx).search(q);
            } catch (RemoteException e) {
                // tenta próximo
            }
        }
        throw new RemoteException("All barrels unavailable");
    }

    @Override
    public int inlinks(String url) throws RemoteException {
        return pick().inlinks(url);
    }

    @Override
    public StatsSnapshot stats() throws RemoteException {
        // 1) métricas do Barrel (numDocs, numTerms, numPostings)
        StatsSnapshot b = pick().stats();

        // 2) métricas do DownloaderManager (pagesIndexed, urlsInQueue, activeDownloaders)
        StatsSnapshot d = downloader.stats();

        // 3) combina
        b.pagesIndexed      = d.pagesIndexed;
        b.urlsInQueue       = d.urlsInQueue;
        b.activeDownloaders = d.activeDownloaders;
        return b;
    }

}
