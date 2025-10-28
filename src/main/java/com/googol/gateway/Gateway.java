package com.googol.gateway;

import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Gateway extends Remote {
    void indexUrl(String url) throws RemoteException;
    SearchResult search(SearchQuery query) throws RemoteException;
    int inlinks(String url) throws RemoteException;
    String stats() throws RemoteException;
}
