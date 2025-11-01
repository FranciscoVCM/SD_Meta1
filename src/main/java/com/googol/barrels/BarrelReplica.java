package com.googol.barrels;

import com.googol.index.InvertedIndex;
import com.googol.model.CrawlResult;
import com.googol.model.PageDocument;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class BarrelReplica extends UnicastRemoteObject implements Barrel {

    private final InvertedIndex index = new InvertedIndex();

    public BarrelReplica() throws RemoteException { }

    @Override
    public synchronized void append(CrawlResult r) throws RemoteException {
        if (r == null || r.url == null) return;

        PageDocument doc = new PageDocument();
        doc.url      = r.url;
        doc.title    = (r.title == null || r.title.isBlank()) ? r.url : r.title;
        doc.text     = (r.text == null) ? "" : r.text;
        doc.snippet  = r.snippet;
        doc.outlinks = r.outlinks;   // pode ser vazio neste exercício

        index.add(doc);
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        return index.search(q);
    }

    // antes
    @Override
    public synchronized int inlinks(String url) throws RemoteException {
        return index.inlinks(url);
    }


    @Override
    public synchronized StatsSnapshot stats() throws RemoteException {
        return index.stats();

    }


}

