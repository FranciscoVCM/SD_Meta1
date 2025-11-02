package com.googol.barrels;

import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Barrel extends Remote {
    void append(CrawlResult result) throws RemoteException;

    SearchResult search(SearchQuery query) throws RemoteException;

    int inlinks(String url) throws RemoteException;

    StatsSnapshot stats() throws RemoteException;

    List<String> backlinks(String url) throws RemoteException;
}
