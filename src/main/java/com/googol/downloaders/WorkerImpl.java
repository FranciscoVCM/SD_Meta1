package com.googol.downloaders;

import com.googol.model.CrawlResult;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class WorkerImpl extends UnicastRemoteObject implements Worker {

    public WorkerImpl() throws RemoteException {}

    @Override
    public String getTask() throws RemoteException {
        return null; // workers nunca pedem tarefas ao gateway
    }

    @Override
    public void submitResult(CrawlResult r) throws RemoteException {
    }
}

