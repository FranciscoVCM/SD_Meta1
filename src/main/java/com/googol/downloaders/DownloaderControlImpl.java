package com.googol.downloaders;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import com.googol.model.StatsSnapshot;

public class DownloaderControlImpl extends UnicastRemoteObject implements DownloaderControl {
    private final DownloaderManager dm;

    public DownloaderControlImpl(DownloaderManager dm) throws RemoteException {
        super(0); // porta aleatória
        this.dm = dm;
    }

    @Override
    public StatsSnapshot downloaderStats() throws RemoteException {
        return dm.stats();
    }

    @Override
    public void enqueue(String url, int depth) throws RemoteException {
        dm.enqueue(url, depth);
        System.out.println("[Standalone] received seed: " + url + " (d=" + depth + ")");
    }
}