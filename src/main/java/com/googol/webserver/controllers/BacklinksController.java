package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BacklinksController {

    private final GatewayService gateway;

    public BacklinksController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/backlinks")
    public String backlinks(@RequestParam(required = false) String url, Model model) {

        if (url == null) {
            return "backlinks";
        }

        var list = gateway.backlinks(url);

        if (list == null) {
            model.addAttribute("error", "Gateway offline.");
        } else {
            model.addAttribute("url", url);
            model.addAttribute("backlinks", list);
        }

        return "backlinks";
    }
}
