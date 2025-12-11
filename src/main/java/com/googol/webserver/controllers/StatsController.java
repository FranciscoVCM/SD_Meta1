package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import com.googol.model.StatsSnapshot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StatsController {

    private final GatewayService gateway;

    public StatsController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/stats")
    public String stats(Model model) {

        StatsSnapshot s = gateway.stats();

        if (s == null) {
            model.addAttribute("error", "Gateway offline.");
            return "stats";
        }

        model.addAttribute("s", s);
        return "stats";
    }
}
