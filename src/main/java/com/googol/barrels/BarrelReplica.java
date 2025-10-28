package com.googol.barrels;

import com.googol.index.InvertedIndex;
import com.googol.index.LinkGraph;
import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class BarrelReplica extends UnicastRemoteObject implements Barrel {

    private final InvertedIndex index = new InvertedIndex();
    private final LinkGraph linkGraph = new LinkGraph();

    public BarrelReplica() throws RemoteException {
        super();
    }

    @Override
    public synchronized void append(CrawlResult r) throws RemoteException {
        // TODO: index.add(…); linkGraph.addInlink(…)
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        // TODO: procurar no índice e devolver 10 resultados
        return new SearchResult();
    }

    @Override
    public synchronized int inlinks(String url) throws RemoteException {
        return linkGraph.inlinks(url);
    }

    @Override
    public synchronized StatsSnapshot stats() throws RemoteException {
        return new StatsSnapshot();
    }
}
