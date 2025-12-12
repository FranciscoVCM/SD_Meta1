package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatsController {

    private final GatewayService gateway;

    public StatsController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/stats")
    public String page(Model model) {
        model.addAttribute("gatewayOnline", gateway.isOnline());
        return "stats"; // página será atualizada via WebSocket
    }
}

