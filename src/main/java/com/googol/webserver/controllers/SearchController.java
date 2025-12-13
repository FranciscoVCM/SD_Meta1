package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import com.googol.webserver.rest.AIService;
import com.googol.model.SearchResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SearchController {

    private final GatewayService gateway;
    private final AIService ai;

    public SearchController(GatewayService gateway, AIService ai) {
        this.gateway = gateway;
        this.ai = ai;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(defaultValue = "") String terms,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        if (terms.isBlank()) {
            model.addAttribute("terms", "");
            model.addAttribute("result", null);
            model.addAttribute("analysis", "");
            return "search";
        }

        SearchResult result = gateway.search(terms, page);

        // Recolher até 50 snippets (globais, não só desta página)
        List<String> snippets = result.items.stream()
                .map(i -> i.snippet == null ? "" : i.snippet)
                .filter(s -> !s.isBlank())
                .limit(50)
                .toList();

        String analysis = ai.analyze(terms, snippets);

        model.addAttribute("terms", terms);
        model.addAttribute("result", result);
        model.addAttribute("analysis", analysis);

        return "search";
    }
}
