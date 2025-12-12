package com.googol.webserver.websocket;

import com.googol.model.StatsSnapshot;
import com.googol.webserver.rmi.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatsWebSocket {

    @Autowired
    private GatewayService gateway;

    @Autowired
    private SimpMessagingTemplate messaging;

    @Scheduled(fixedDelay = 2000)
    public void pushStats() {
        try {
            StatsSnapshot s = gateway.stats();
            if (s != null) {
                messaging.convertAndSend("/topic/stats", s);
            }
        } catch (Exception ignored) {}
    }
}

