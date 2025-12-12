package com.googol.downloaders;

import com.googol.model.CrawlResult;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Worker extends Remote {
    String getTask() throws RemoteException;
    void submitResult(CrawlResult r) throws RemoteException;
}