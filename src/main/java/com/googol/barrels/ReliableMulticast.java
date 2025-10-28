package com.googol.barrels;

import com.googol.model.CrawlResult;
import java.util.List;

public class ReliableMulticast {
    public void fanout(List<Barrel> replicas, CrawlResult r) {
        // TODO: enviar para todas as réplicas com retries
    }
}