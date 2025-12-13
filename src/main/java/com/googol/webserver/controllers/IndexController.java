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

    // === HOME PAGE ===
    @GetMapping("/")
    public String home(Model model) {
        // mensagens opcionais vindas do POST
        return "index"; // renderiza index.html
    }

    // === INDEX URL ===
    @PostMapping("/index")
    public String indexUrl(@RequestParam String url, Model model) {

        boolean ok = gateway.indexUrl(url);

        if (!ok) {
            model.addAttribute("error", "Gateway offline — não foi possível indexar a URL.");
        } else {
            model.addAttribute("msg", "URL enviada para indexação com sucesso.");
        }

        return "index"; // volta ao menu inicial
    }
}
