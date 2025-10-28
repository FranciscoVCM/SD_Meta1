package com.googol.gateway;

import com.googol.barrels.Barrel;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GatewayServer extends UnicastRemoteObject implements Gateway {

    private final List<Barrel> barrels;
    private final AtomicInteger rr = new AtomicInteger();

    public GatewayServer(List<Barrel> barrels) throws RemoteException {
        super();
        this.barrels = barrels;
    }

    private Barrel pick() {
        return barrels.get(Math.abs(rr.getAndIncrement() % barrels.size()));
    }

    @Override
    public void indexUrl(String url) throws RemoteException {
        // TODO: colocar URL na fila/crawler
        System.out.println("Index request: " + url);
    }

    @Override
    public SearchResult search(SearchQuery q) throws RemoteException {
        if (barrels.isEmpty()) return new SearchResult();
        return pick().search(q);
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
