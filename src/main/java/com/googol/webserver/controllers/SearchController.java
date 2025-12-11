package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import com.googol.model.SearchResult;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class SearchController {

    private final GatewayService gateway;

    public SearchController(GatewayService gateway) {
        this.gateway = gateway;
    }

    @GetMapping("/")
    public String home() {
        return "search";
    }

    @GetMapping("/search")
    public String search(@RequestParam String terms,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {

        SearchResult result = gateway.search(terms, page);

        if (result == null) {
            model.addAttribute("error", "Gateway offline — tente novamente mais tarde.");
            return "search";
        }

        model.addAttribute("terms", terms);
        model.addAttribute("result", result);
        return "results";
    }
}
