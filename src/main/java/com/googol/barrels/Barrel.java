package com.googol.barrels;

import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Barrel extends Remote {

    void append(CrawlResult r) throws RemoteException;

    SearchResult search(SearchQuery q) throws RemoteException;

    int inlinks(String url) throws RemoteException;

    List<String> backlinks(String url) throws RemoteException;

    StatsSnapshot barrelStats() throws RemoteException;

    String getName() throws RemoteException;
}
