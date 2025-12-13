package com.googol.webserver.websocket;

import com.googol.model.StatsSnapshot;
import com.googol.webserver.rmi.GatewayService;
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

    @Scheduled(fixedRate = 1200)
    public void publishStats() {
        StatsSnapshot s = gateway.stats();
        if (s == null) return;

        // TOP QUERIES
        List<Map<String,Object>> top = new ArrayList<>();
        for (String raw : s.topQueries) {
            String[] p = raw.split("\\(");
            if (p.length < 2) continue;

            top.add(Map.of(
                    "term", p[0].trim(),
                    "count", Integer.parseInt(p[1].replace(")", "").trim())
            ));
        }

        // BARRELS
        List<Map<String,Object>> barrels = new ArrayList<>();
        for (var e : s.barrelNumDocs.entrySet()) {
            barrels.add(Map.of(
                    "name", e.getKey(),
                    "docs", e.getValue(),
                    "avgLatency", s.barrelAvgLatencySec.getOrDefault(e.getKey(), 0.0)
            ));
        }

        // PACKET
        Map<String,Object> packet = Map.of(
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
