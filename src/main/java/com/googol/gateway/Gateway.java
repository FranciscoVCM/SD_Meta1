package com.googol.gateway;

import com.googol.downloaders.WebCrawler;
import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.googol.downloaders.Worker;

public interface Gateway extends Remote {

    void indexUrl(String url) throws RemoteException;

    // === API para workers ===
    String getTask() throws RemoteException;
    void submitResult(CrawlResult r) throws RemoteException;
    void registerDownloader(com.googol.downloaders.Worker worker) throws RemoteException;

    // === API antiga ===
    SearchResult search(SearchQuery q) throws RemoteException;
    int inlinks(String url) throws RemoteException;
    List<String> backlinks(String url) throws RemoteException;
    StatsSnapshot stats() throws RemoteException;
}
