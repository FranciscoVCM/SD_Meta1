package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LinksController {

    private final GatewayService gateway;

    public LinksController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/inlinks")
    public String inlinks(@RequestParam String url, Model model) {

        int count = gateway.inlinks(url);
        if (count < 0) {
            model.addAttribute("error", "Gateway offline.");
        } else {
            model.addAttribute("url", url);
            model.addAttribute("inlinks", count);
        }

        return "inlinks";
    }

    @GetMapping("/backlinks")
    public String backlinks(@RequestParam String url, Model model) {

        var list = gateway.backlinks(url);
        if (list.isEmpty()) {
            model.addAttribute("error", "Gateway offline ou sem backlinks.");
        }

        model.addAttribute("url", url);
        model.addAttribute("backlinks", list);
        return "backlinks";
    }
}
