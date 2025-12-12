package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InlinksController {

    private final GatewayService gateway;

    public InlinksController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/inlinks")
    public String inlinks(@RequestParam(required = false) String url, Model model) {

        if (url == null) {
            return "inlinks";
        }

        int count = gateway.inlinks(url);

        if (count < 0) {
            model.addAttribute("error", "Gateway offline.");
        } else {
            model.addAttribute("url", url);
            model.addAttribute("inlinks", count);
        }

        return "inlinks";
    }
}
