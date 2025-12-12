package com.googol.webserver.websocket;

import com.googol.webserver.rmi.GatewayService;
import com.googol.model.StatsSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StatsPublisher {

    @Autowired
    private SimpMessagingTemplate msg;

    @Autowired
    private GatewayService gateway;

    @Scheduled(fixedRate = 1500)
    public void publishStats() {
        StatsSnapshot s = gateway.stats();
        if (s == null)
            return;
        msg.convertAndSend("/topic/queue", s.urlsInQueue);
        // -------------------------------
        // TOP QUERIES
        // -------------------------------
        List<Object> top = new ArrayList<>();
        for (String q : s.topQueries) {
            String[] parts = q.split("\\s+\\(");
            if (parts.length < 2) continue;

            String term = parts[0];
            int count = Integer.parseInt(parts[1].replace(")", ""));

            top.add(Map.of("term", term, "count", count));
        }

        // -------------------------------
        // BARRELS
        // -------------------------------
        List<Object> barrels = new ArrayList<>();

        for (var e : s.barrelNumDocs.entrySet()) {

            String name = e.getKey();
            int docs = e.getValue();

            double latency = s.barrelAvgLatencySec.getOrDefault(name, -1.0);

            barrels.add(Map.of(
                    "name", name,
                    "docs", docs,
                    "avgLatency", latency
            ));
        }

        // -------------------------------
        // CONSTRUIR OBJETO FINAL PARA O BROWSER
        // -------------------------------
        var packet = Map.of(
                "topQueries", top,
                "barrels", barrels,
                "system", Map.of(
                        "docs", s.numDocs,
                        "terms", s.numTerms,
                        "postings", s.numPostings
                ),
                "lastSearchMs", s.lastSearchMs
        );

        msg.convertAndSend("/topic/stats", packet);
    }
}
