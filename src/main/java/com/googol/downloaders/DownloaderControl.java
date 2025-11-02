package com.googol.downloaders;

import java.rmi.Remote;
import java.rmi.RemoteException;
import com.googol.model.StatsSnapshot;

public interface DownloaderControl extends Remote {
    void enqueue(String url, int depth) throws RemoteException;
    StatsSnapshot downloaderStats() throws RemoteException;
}