package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class IndexController {

    private final GatewayService gateway;

    public IndexController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @PostMapping("/index")
    public String indexUrl(@RequestParam String url, Model model) {

        boolean ok = gateway.indexUrl(url);

        if (!ok) {
            model.addAttribute("error", "Gateway offline — não foi possível indexar.");
        } else {
            model.addAttribute("msg", "URL submetida para indexação.");
        }

        return "search";
    }
}
