package com.googol.barrels;

import com.googol.model.CrawlResult;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Fanout at least once para todas as réplicas Barrel.
 * – Retenta por réplica com backoff exponencial leve.
 * – Devolve true se pelo menos UMA réplica receber (útil para métricas).
 */
public class ReliableMulticast {

    private static final int MAX_TRIES = 5;
    private static final long BASE_SLEEP_MS = 100;

    /** Envia r para todas as réplicas; retorna true se pelo menos uma aceitar. */
    public boolean fanout(List<Barrel> replicas, CrawlResult r) {
        if (replicas == null || replicas.isEmpty() || r == null) return false;

        boolean someSuccess = false;

        for (Barrel b : replicas) {
            int tries = 0;
            while (tries++ < MAX_TRIES) {
                try {
                    b.append(r);
                    someSuccess = true; // pelo menos esta réplica recebeu
                    break;              // segue para a réplica seguinte
                } catch (RemoteException e) {
                    long sleep = BASE_SLEEP_MS * tries;
                    System.err.println("[RMcast] append failed (" + tries + "/" + MAX_TRIES + "): " + e);
                    try { Thread.sleep(sleep); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return someSuccess; }
                }
            }
        }

        return someSuccess;
    }
}
