// src/main/java/com/googol/gateway/Gateway.java
package com.googol.gateway;

import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.googol.downloaders.DownloaderControl;

public interface Gateway extends Remote {
    void indexUrl(String url) throws RemoteException;

    SearchResult search(SearchQuery query) throws RemoteException;

    int inlinks(String url) throws RemoteException;

    StatsSnapshot stats() throws RemoteException;
    //(Ex.5)
    List<String> backlinks(String url) throws RemoteException;
    void registerDownloader(DownloaderControl dc) throws RemoteException;

}
