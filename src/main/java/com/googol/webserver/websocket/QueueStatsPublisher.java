package com.googol.webserver.websocket;

import com.googol.webserver.rmi.GatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueStatsPublisher {

    @Autowired
    private GatewayService gateway;

    @Autowired
    private SimpMessagingTemplate msg;

    @Scheduled(fixedRate = 1000)
    public void sendQueueSize() {
        var s = gateway.stats();
        if (s == null) return;

        msg.convertAndSend("/topic/queue", "" + s.urlsInQueue);
    }
}
